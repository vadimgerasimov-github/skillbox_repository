package searchengine.services.indexing;

import org.jsoup.nodes.Document;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import searchengine.model.Page;

import java.io.IOException;

@Service
public interface PageHandlerService {
  void addPageIfNotExists(Page page);
  @Retryable
  Document getDocument(String url) throws IOException;
}
