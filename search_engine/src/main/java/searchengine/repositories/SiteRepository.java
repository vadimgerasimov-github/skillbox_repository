package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

import java.util.Optional;
import java.util.Set;

@Repository
public interface SiteRepository extends JpaRepository <Site, Integer> {

    Optional<Site> findOneByUrl (String url);

    @Modifying
    @Query(nativeQuery = true, value = "TRUNCATE TABLE site CASCADE")
    void clearDb();

}
