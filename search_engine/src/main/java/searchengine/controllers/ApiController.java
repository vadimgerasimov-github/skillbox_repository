package searchengine.controllers;

import jico.ImageReadException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;

import java.io.IOException;
import java.net.URISyntaxException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() throws IOException, ImageReadException {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }


    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(String url) throws IOException, InterruptedException {
        return ResponseEntity.ok(indexingService.indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search
            (@RequestParam(value = "query") String query,
             @RequestParam(value = "site"
                     , defaultValue = "allSites"
                     , required = false
             ) String site,
             @RequestParam(value = "offset") Integer offset,
             @RequestParam(value = "limit") Integer limit) throws IOException, InterruptedException, URISyntaxException {


        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }
}