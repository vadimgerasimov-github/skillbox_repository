package searchengine.services.search;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import searchengine.config.SnippetSettings;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResult;
import searchengine.model.*;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.dictionary.Dictionary;
import searchengine.services.snippet.SnippetConstructor;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Data
public class SearchServiceImpl implements SearchService {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final Dictionary dictionary;
    private final SnippetSettings snippetSettings;
    private final EntityManager entityManager;
    private String site;
    private Set<String> allQueryLemmas;
    private Float currentMaxRank;
    private int currentResultsCount;
    private static String prevSite;

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {

        log.info("Search query: {}", query);

        if (offset > 0) {
            site = prevSite;
        }

        setSite(site);

        List<String> lemmasForPageSearch = getLemmasForPageSearch(query);

        Map<Page, Float> pageRelevanceMap = !lemmasForPageSearch.isEmpty() ?
                getPageRelevanceMap(lemmasForPageSearch, offset, limit) : new HashMap<>();

        List<SearchResult> searchResults;

        try {
            searchResults = getResultList(pageRelevanceMap);
        } catch (InterruptedException e) {
            return new SearchResponse(false, "Search was interrupted");
        }

        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(lemmasForPageSearch.isEmpty() ? 0 : getCurrentResultsCount());
        response.setData(searchResults);

        return response;
    }

    private List<String> getLemmasForPageSearch(String query) {
        String[] queryWords = dictionary.getWordsArray(query);
        Set<String> lemmaSet = dictionary.getLemmaMap(queryWords).keySet();
        Map<String, Integer> allQueryLemmas = new HashMap<>();
        for (String lemma : lemmaSet) {
            String SQL = "SELECT SUM(l.frequency) FROM search_engine.lemma l WHERE l.lemma = :lemma";
            Query frequencyQuery = entityManager.createNativeQuery(SQL);
            frequencyQuery.setParameter("lemma", lemma);
            Object frequency = frequencyQuery.getSingleResult();
            if (frequency != null) {
                allQueryLemmas.put(lemma, ((BigDecimal) frequency).intValue());
            }
        }

        setAllQueryLemmas(allQueryLemmas.keySet());

        Map<String, Integer> lemmasForPageSearch;
        Map<String, Integer> rareQueryLemmas;

        long pageCount = pageRepository.count();

        rareQueryLemmas = allQueryLemmas.entrySet().stream()
                .filter(e -> e.getValue() <= pageCount * 0.5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        lemmasForPageSearch = !rareQueryLemmas.isEmpty() ? rareQueryLemmas : allQueryLemmas;

        return lemmasForPageSearch.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    private Map<Page, Float> getPageRelevanceMap(List<String> queryLemmas, int offset, int limit) {
        String SQL = getQuery();
        Query query = entityManager.createQuery(SQL, Tuple.class);
        query.setParameter("lemmas", queryLemmas);
        query.setParameter("lemmaCount", ((long) queryLemmas.size()));
        setCurrentResultsCount(query.getResultList().size());
        query.setMaxResults(limit);
        query.setFirstResult(offset);
        Map<Page, Float> pagesWithRelevance = new LinkedHashMap<>();
        List<Tuple> results = query.getResultList();
        if (results.isEmpty()) return new HashMap<>();
        for (Tuple tuple : results) {
            Page page = (Page) tuple.get(0);
            Double absoluteRelevance = tuple.get("abs_relevance", Double.class);
            pagesWithRelevance.put(page, absoluteRelevance.floatValue());
        }
        if (offset == 0) {
            currentMaxRank = pagesWithRelevance.values().stream().findFirst().get();
        }
        logRelevanceInfo(pagesWithRelevance);
        return pagesWithRelevance;
    }

    private String getQuery() {
        String SQL = "SELECT p, " +
                "SUM(i.rank) AS abs_relevance " +
                "FROM Page p  " +
                "JOIN Index i ON p.id = i.page " +
                "JOIN Lemma l ON i.lemma = l.id " +
                "AND l.lemma IN (:lemmas) " +
                "GROUP BY p.id " +
                "HAVING COUNT(l.lemma) = :lemmaCount " +
                "ORDER BY abs_relevance DESC, p.path ASC";
        if (!site.equals("allSites")) {
            int siteId = siteRepository.findOneByUrl(site).get().getId();
            SQL = SQL.replaceAll("(?=GROUP BY p.id)", "WHERE p.site = " + siteId + " ");
        }
        return SQL;
    }

    private void logRelevanceInfo(Map<Page, Float> pagesWithRelevance) {
        pagesWithRelevance.replaceAll((key, value) -> value / currentMaxRank);
        pagesWithRelevance.forEach((key, value) -> log.info("{}, relevance = {}",
                key.getSite().getUrl().concat(key.getPath()), value));
    }

    private List<SearchResult> getResultList(Map<Page, Float> pagesWithRelMap)
            throws InterruptedException {

        List<SearchResult> results = Collections.synchronizedList(new ArrayList<>());
        List<Page> resultPageList = new ArrayList<>(pagesWithRelMap.keySet());

        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (Page page : resultPageList) {
            service.submit(() -> {
                results.add(getResult(pagesWithRelMap, page));
            });
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.DAYS);

        results.sort(Comparator.comparing(SearchResult::getRelevance).reversed()
                .thenComparing(SearchResult::getUri));

        prevSite = site;

        return results;
    }

    private SearchResult getResult(Map<Page, Float> pagesWithRelMap, Page page) {
        SearchResult result = new SearchResult();

        Site site = page.getSite();
        String content = page.getContent();
        String title = Jsoup.parse(content).title();

        if (title.isBlank()) {
            title = Jsoup.parse(content).select("h1, h2, h3, h4, h5, h6, [class*='name']")
                    .stream()
                    .map(Element::text).filter(t -> !t.isBlank())
                    .findFirst()
                    .orElse("");
        }

        SnippetConstructor constructor = new SnippetConstructor(content, allQueryLemmas, dictionary, snippetSettings);
        String snippet = constructor.getSnippet();

        if (snippet.isBlank()) {
            snippet = title.replaceAll("(?<=[^.?!])\\z", ".");
        }

        result.setSite(site.getUrl().replaceAll("/$", ""));
        result.setSiteName(site.getName());
        result.setUri(page.getPath());
        result.setRelevance(pagesWithRelMap.get(page));
        result.setTitle(title);
        result.setSnippet(snippet);
        result.setImage("/assets/favicons/" + site.getName() + ".png");
        return result;
    }
}