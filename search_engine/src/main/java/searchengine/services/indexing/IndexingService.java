package searchengine.services.indexing;

import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingResponse;

import java.io.IOException;

@Service
public interface IndexingService {

    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
    void addLastError(String lastError);
    IndexingResponse indexPage(String url) throws IOException, InterruptedException;
}