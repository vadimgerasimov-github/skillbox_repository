package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

@Service
@RequiredArgsConstructor
public class UpdatesServiceImpl implements UpdatesService {

    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Override
    public void insertOrUpdateLemma(Lemma lemma) {
        lemmaRepository.saveOrUpdate(lemma);
    }

    @Override
    public void clearDb() {
        siteRepository.clearDb();
    }
    @Override
    public void deleteByPathAndSite(String path, Site site) {
        pageRepository.deleteByPathAndSite(path, site);
        pageRepository.flush();
    }
}
