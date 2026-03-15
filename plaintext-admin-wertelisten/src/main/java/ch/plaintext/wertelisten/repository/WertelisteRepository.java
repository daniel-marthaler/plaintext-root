package ch.plaintext.wertelisten.repository;

import ch.plaintext.wertelisten.entity.Werteliste;
import ch.plaintext.wertelisten.entity.WertelisteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WertelisteRepository extends JpaRepository<Werteliste, WertelisteId> {

    Optional<Werteliste> findByKeyAndMandat(String key, String mandat);

    List<Werteliste> findByMandat(String mandat);

    @Query("SELECT DISTINCT w.key FROM Werteliste w WHERE w.mandat = :mandat ORDER BY w.key")
    List<String> findAllKeysByMandat(@Param("mandat") String mandat);

    @Query("SELECT COUNT(w) > 0 FROM Werteliste w WHERE w.key = :key AND w.mandat = :mandat")
    boolean existsByKeyAndMandat(@Param("key") String key, @Param("mandat") String mandat);

    void deleteByKeyAndMandat(String key, String mandat);
}
