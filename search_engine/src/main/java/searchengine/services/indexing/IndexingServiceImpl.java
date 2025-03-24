package searchengine.services.indexing;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.dictionary.Dictionary;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Data
public class IndexingServiceImpl implements IndexingService {

    private final UpdateService updateService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesList sitesList;
    private ExecutorService executorService;
    private final Dictionary dictionary;
    private final Connection connectionProperties;
    private Map<Site, String> errorsMap;


    @Override
    public IndexingResponse startIndexing() {

        if (isIndexingNow()) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }

        errorsMap = new HashMap<>();
        updateDB(sitesList.getSites());
        List<Site> sites = siteRepository.findAll();

        CompletableFuture.runAsync(() -> {
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (Site site : sites) {
                executorService.submit(() -> {
                    indexSite(site);
                });
            }
            executorService.shutdown();
        });

        return new IndexingResponse(true);
    }

    private void indexSite(Site site) {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        CompletableFuture.runAsync(() -> {
            {
                String homePage = getHomePage(site.getUrl());
                forkJoinPool.execute(new SiteHandlerRecursiveAction("/", homePage, site));
                forkJoinPool.shutdown();
            }
        });
        try {
            forkJoinPool.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            forkJoinPool.shutdownNow();
        } finally {
            updateSite(site);
        }
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
    public IndexingResponse indexPage(String url) {
        try {
            String path = new URL(url).getPath();
            Optional<searchengine.config.Site> configSiteOptional = sitesList.getSites().stream()
                    .filter(s -> getHomePage(url).contains(getHomePage(s.getUrl())))
                    .findAny();
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
                updateService.deleteByPathAndSite(path, site);
                Document.OutputSettings outputSettings = new Document.OutputSettings();
                outputSettings.prettyPrint(false);
                Document document = getDocument(url);
                Page page = new Page(site, path, document.connection().response().statusCode(), document.html());
                addPageIfNotExists(page);
                return new IndexingResponse(true);
            } else
                return new IndexingResponse(false, "Данная страница находится за пределами сайтов, " +
                        "указанных в конфигурационном файле");
        } catch (IOException e) {
            return new IndexingResponse(false, defineErrorMessage(e));
        }
    }

    private boolean isIndexingNow() {
        return executorService != null && !executorService.isTerminated();
    }

    public void updateDB(List<searchengine.config.Site> sites) {

        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();

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
            if (pageRepository.existsByPathAndSite(path, site)) {
                return;
            }
            try {
                Thread.sleep(500);
                processPage();
            } catch (Exception e) {
                String errorMessage = defineErrorMessage(e);
                if (e instanceof SocketException || e instanceof UnknownHostException) {
                    addLastError(errorMessage);
                } else {
                    log.info("{}: {}", site.getUrl().concat(path), errorMessage);
                }
                if (e instanceof HttpStatusException) {
                    Page emptyPage = new Page(site, path, ((HttpStatusException) e).getStatusCode(), "");
                    if (isIndexingNow()) addPageIfNotExists(emptyPage);
                }
            }
        }

        private void processPage() throws IOException {
            Document document = getDocument(homePage.concat(path));
            Page page = new Page();
            page.setSite(site);
            page.setPath(path);
            page.setCode(document.connection().response().statusCode());
            page.setContent(document.html());
            if (isIndexingNow()) {
                addPageIfNotExists(page);
            } else return;
            Set<String> relevantLinks = getRelevantLinks(getLinksFromDoc(document), homePage, site);
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

    private String defineErrorMessage(Exception e) {
        if (e instanceof HttpStatusException) return "Page not found";
        if (e instanceof SSLHandshakeException) return "Unable to connect to site cause certificate problems";
        if (e instanceof SocketException) return "Network is unreachable";
        if (e instanceof UnknownHostException) return "Unable to connect to site";
        if (e instanceof SocketTimeoutException) return "Page connection timeout";
        if (e instanceof CancellationException | e instanceof InterruptedException)
            return "Indexation was interrupted";
        return "";
    }


    public void addPageIfNotExists(Page page) {

        try {
            pageRepository.save(page);
        } catch (ConstraintViolationException | DataIntegrityViolationException e) {
            log.info("Page {} is already in database", page.getSite().getUrl().concat(page.getPath()));
            return;
        }

        if (page.getCode() == 200) {
            List<Index> indexList = new ArrayList<>();

            String clearedText = dictionary.removeTags(page.getContent());

            String[] text = dictionary.getWordsArray(clearedText);
            Map<String, Integer> lemmasMap = dictionary.getLemmaMap(text);

            for (Map.Entry<String, Integer> entry : lemmasMap.entrySet()) {
                String lemma = entry.getKey();
                Float rank = Float.valueOf(entry.getValue());

                Lemma lemma1 = new Lemma(page.getSite(), lemma, 1);

                updateService.insertOrUpdateLemma(lemma1);

                Lemma l = lemmaRepository.findByLemmaAndSite(lemma, page.getSite());
                indexList.add(new Index(page, l, rank));
            }
            indexRepository.saveAll(indexList);
        }

        Site site = page.getSite();
        site.setDateTime(LocalDateTime.now());
        siteRepository.save(site);

    }

    @Retryable()
    public Document getDocument(String url) throws IOException {
        Document document;
        document = Jsoup.connect(url)
                .userAgent(connectionProperties.getUserAgent())
                .referrer(connectionProperties.getReferrer())
                .timeout(connectionProperties.getTimeout())
                .get();
        return document;
    }

    private HashSet<String> getRelevantLinks(Set<String> links, String homePage, Site site) {
        return links.stream()
                .map(l -> l.replace("www.", ""))
                .filter(l -> (l.replace(homePage, "")).startsWith("/"))
                .filter(l -> l.startsWith(homePage))
                .filter(l -> !l.equals(homePage))
                .filter(l -> !pageRepository.existsByPathAndSite(l.replace(homePage, ""), site))
                .filter(l -> !l.matches(".*(sort|login|\\?|goout\\.php|lang|form|rss|#).*"))
                .filter(l -> !l.matches("^\\S+(\\.(?i)(jpe?g|png|gif|bmp|pdf|doc?x))$"))
                .map(l -> l.replaceAll("/$", ""))
                .collect(Collectors.toCollection(HashSet::new));
    }

}