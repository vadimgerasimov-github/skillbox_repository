package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

@Repository
public interface PageRepository extends JpaRepository <Page, Integer> {

    void deleteByPathAndSite(String path, Site site);

    boolean existsByPathAndSite(String path, Site site);

    int countBySite(Site site);

}