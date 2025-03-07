package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Transactional()
    Lemma findByLemmaAndSite(String lemma, Site site);

    @Transactional
    @Modifying
    @Query(
            nativeQuery = true,
            value = "INSERT INTO search_engine.lemma " +
                    "(site_id, lemma, frequency) " +
                    "VALUES (:#{#lemma.site}, :#{#lemma.lemma}, :#{#lemma.frequency}) " +
                    "ON DUPLICATE KEY UPDATE frequency = frequency + 1"
    )
    void saveOrUpdate(@Param("lemma") Lemma lemma);

    int countBySite(Site site);

}
