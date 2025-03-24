package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;


    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    public IndexingResponse startIndexing() {
        return indexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing() {
        return indexingService.stopIndexing();
    }


    @PostMapping("/indexPage")
    public IndexingResponse indexPage(String url) {
        return indexingService.indexPage(url);
    }

    @GetMapping("/search")
    public SearchResponse search
            (@RequestParam(value = "query") String query,
             @RequestParam(value = "site"
                     , defaultValue = "allSites"
                     , required = false
             ) String site,
             @RequestParam(value = "offset") Integer offset,
             @RequestParam(value = "limit") Integer limit) {

        return searchService.search(query, site, offset, limit);
    }
}