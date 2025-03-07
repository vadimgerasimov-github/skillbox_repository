package searchengine.services.search;

import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;

import java.io.IOException;
import java.net.URISyntaxException;

@Service
public interface SearchService {
    SearchResponse search (String query, String site, Integer offset, Integer limit) throws IOException, InterruptedException, URISyntaxException;
}
