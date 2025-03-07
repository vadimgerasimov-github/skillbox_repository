package searchengine.services.indexing;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Data
public class IndexingServiceImpl implements IndexingService {

    private final PageHandlerService pageHandlerService;
    private ExecutorService executorService;
    private final Filter filter;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final Connection connectionProperties;
    private final SitesList sitesList;
    private Map<Site, String> errorsMap;

    @Override
    public IndexingResponse startIndexing() {

        if (isIndexingNow()) {return new IndexingResponse(false, "Индексация уже запущена");}

        errorsMap = new HashMap<>();

        CompletableFuture.runAsync(() -> {

            updateDB(sitesList.getSites());

            long start = System.currentTimeMillis();

            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            for (Site site : siteRepository.findAll()) {
                indexSitePages(site);
            }

            executorService.shutdown();

            try {
                executorService.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }

            log.info("Индексация заняла {} секунд(ы)", (System.currentTimeMillis() - start) / 1000);
        });

        return new IndexingResponse(true);
    }

    private void indexSitePages(Site site) {
        executorService.submit(() -> {

            ForkJoinPool forkJoinPool = new ForkJoinPool();

            CompletableFuture.runAsync(() -> {
                String homePage = getHomePage(site.getUrl());
                forkJoinPool.submit(new SiteHandlerRecursiveAction("/", homePage, site));
                forkJoinPool.shutdown();
            });

            try {
                forkJoinPool.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                forkJoinPool.shutdownNow();
            } finally {
                updateSite(site);
            }
        });
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (isIndexingNow()) {
            addLastError("Индексация остановлена пользователем");
            executorService.shutdownNow();
            return new IndexingResponse(true);
        } else {
            return new IndexingResponse(false, "Индексация сейчас не запущена");
        }
    }

    public void addLastError(String lastError) {
        siteRepository.findAll().stream().filter(s -> s.getSiteStatus().equals(SiteStatus.INDEXING)).forEach(s -> errorsMap.put(s, lastError));
    }

    private void updateSite(Site site) {

        if (pageRepository.countBySite(site) <= 1) {
            errorsMap.put(site, "Unable to connect to the site");
        }

        String lastError = errorsMap.getOrDefault(site, "");
        SiteStatus status = lastError.isEmpty() ? SiteStatus.INDEXED : SiteStatus.FAILED;

        site.setSiteStatus(status);
        site.setLastError(lastError);
        site.setDateTime(LocalDateTime.now());

        siteRepository.save(site);
    }

    @Override
    public IndexingResponse indexPage(String url) throws IOException {
        String homePage = getHomePage(url);
        String path = new URL(url).getPath();
        Optional<searchengine.config.Site> configSiteOptional = sitesList.getSites().stream().filter(s -> homePage.contains(getHomePage(s.getUrl()))).findAny();
        if (configSiteOptional.isPresent()) {
            searchengine.config.Site configSite = configSiteOptional.get();
            Site site;
            Optional<Site> siteEntityOptional = siteRepository.findOneByUrl(configSite.getUrl());
            if (siteEntityOptional.isPresent()) {
                site = siteEntityOptional.get();
            } else {
                site = getSiteEntity(configSite);
                siteRepository.save(site);
            }
            pageRepository.deleteByPathAndSite(path, site);
            Document.OutputSettings outputSettings = new Document.OutputSettings();
            outputSettings.prettyPrint(false);
            Document document = pageHandlerService.getDocument(url);
            Page page = new Page(site, path, document.connection().response().statusCode(), document.html());
            pageHandlerService.addPageIfNotExists(page);
            return new IndexingResponse(true);
        } else
            return new IndexingResponse(false, "Данная страница находится за пределами сайтов, " + "указанных в конфигурационном файле");
    }

    private boolean isIndexingNow() {
        return executorService != null && !executorService.isTerminated();
    }

    public void updateDB(List<searchengine.config.Site> sites) {
        siteRepository.deleteAll();
        sites.forEach(s -> siteRepository.save(getSiteEntity(s)));
    }

    public Site getSiteEntity(searchengine.config.Site configSite) {
        Site siteEntity = new Site();
        siteEntity.setName(configSite.getName());
        siteEntity.setUrl(configSite.getUrl());
        siteEntity.setSiteStatus(SiteStatus.INDEXING);
        siteEntity.setDateTime(LocalDateTime.now());
        siteEntity.setLastError("");
        return siteEntity;
    }

    @RequiredArgsConstructor
    public class SiteHandlerRecursiveAction extends RecursiveAction {
        private final String path;
        private final String homePage;
        private final Site site;

        @Override
        protected void compute() {
            if (Thread.currentThread().isInterrupted()) {
                this.cancel(true);
            }
            if (pageRepository.existsByPathAndSite(path, site)) {
                return;
            }
            try {
                Thread.sleep(500);
                processPage();
            } catch (HttpStatusException e) {
                log.info(e.getMessage());
                Page emptyPage = new Page(site, path, e.getStatusCode(), "");
                pageHandlerService.addPageIfNotExists(emptyPage);
            } catch (SSLHandshakeException e) {
                errorsMap.put(site, "Unable to connect to site cause certificate problems: {}");
            } catch (IOException e) {
                if (e instanceof SocketException) {
                    addLastError("Network is unreachable");
                    executorService.shutdownNow();
                } else if (e instanceof UnknownHostException) {
                    addLastError("Unable to connect to site");
                    executorService.shutdownNow();
                } else if (e instanceof SocketTimeoutException) {
                    log.info("Page connection timeout: {}", homePage.concat(path));
                }
            } catch (InterruptedException | CancellationException e) {
                log.info(("Page processing was interrupted: {}"), homePage.concat(path));
            }
        }

        private void processPage() throws IOException {
            Document document = pageHandlerService.getDocument(homePage.concat(path));
            Page page = new Page();
            page.setSite(site);
            page.setPath(path);
            page.setCode(document.connection().response().statusCode());
            page.setContent(document.html());
            pageHandlerService.addPageIfNotExists(page);
            Set<String> relevantLinks = filter.getRelevantLinks(getLinksFromDoc(document), homePage, site);
            Set<SiteHandlerRecursiveAction> actionSet = relevantLinks.stream()
                    .map(link -> link.substring(homePage.length()))
                    .map(path -> new SiteHandlerRecursiveAction(path, homePage, site))
                    .collect(Collectors.toSet());
            ForkJoinTask.invokeAll(actionSet);
        }

        private Set<String> getLinksFromDoc(Document document) {
            Elements elements = document.getElementsByTag("a");
            return elements.stream().map(element -> element.attr("abs:href")).collect(Collectors.toCollection(HashSet::new));
        }
    }

    private String getHomePage(String url) {
        return url.replaceAll("(www.)", "");
    }
}