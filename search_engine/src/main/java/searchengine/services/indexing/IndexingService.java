package searchengine.services.indexing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.indexing.IndexingResponse;

@Service
public interface IndexingService {
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
    @Transactional ()
    IndexingResponse indexPage(String url);
}