package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;
import searchengine.config.Connection;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.dictionary.Dictionary;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageHandlerServiceImpl implements PageHandlerService {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final Dictionary dictionary;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final Connection connectionProperties;


    @Override
    public void addPageIfNotExists(Page page) {

        try {
            pageRepository.save(page);
        }
        catch (ConstraintViolationException | DataIntegrityViolationException e) {
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

                lemmaRepository.saveOrUpdate(new Lemma(page.getSite(), lemma, 1));
                Lemma l = lemmaRepository.findByLemmaAndSite(lemma, page.getSite());
                indexList.add(new Index(page, l, rank));
            }
            indexRepository.saveAll(indexList);
        }

        Site site = page.getSite();
        site.setDateTime(LocalDateTime.now());
        siteRepository.save(site);

    }

    public Document getDocument(String url) throws IOException {
        int retryNumber = Objects.requireNonNull(RetrySynchronizationManager.getContext()).getRetryCount();
        if (retryNumber>0) {
        log.info("Retry number {}, to connect to {}", retryNumber, url);}
        return Jsoup.connect(url)
                .userAgent(connectionProperties.getUserAgent())
                .referrer(connectionProperties.getReferrer())
                .timeout(connectionProperties.getTimeout())
                .get();
    }
}