package searchengine.services.indexing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

@Service
public interface UpdatesService {
    @Transactional
    void insertOrUpdateLemma(Lemma lemma);
    @Transactional
    void clearDb();
    @Transactional
    void deleteByPathAndSite(String path, Site site);
}