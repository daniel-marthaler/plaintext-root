package ch.plaintext.anforderungen.repository;

import ch.plaintext.anforderungen.entity.AnforderungApiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnforderungApiSettingsRepository extends JpaRepository<AnforderungApiSettings, Long> {

    Optional<AnforderungApiSettings> findByMandat(String mandat);

    Optional<AnforderungApiSettings> findByApiToken(String apiToken);
}
