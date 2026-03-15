package ch.plaintext.email.persistence;

import ch.plaintext.email.model.EmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailConfigRepository extends JpaRepository<EmailConfig, Long> {

    /**
     * Finds the first email configuration for a mandate.
     * If multiple configurations exist, returns the first one ordered by config name.
     *
     * @deprecated Use findByMandatAndConfigName() to specify which config to use.
     *             This method is kept for backwards compatibility but may return
     *             unexpected results if multiple configs exist for the same mandate.
     */
    @Deprecated
    Optional<EmailConfig> findFirstByMandatOrderByConfigNameAsc(String mandat);

    boolean existsByMandat(String mandat);

    List<EmailConfig> findByMandatOrderByConfigNameAsc(String mandat);

    Optional<EmailConfig> findByMandatAndConfigName(String mandat, String configName);

    boolean existsByMandatAndConfigName(String mandat, String configName);

    List<EmailConfig> findByImapEnabledTrue();
}
