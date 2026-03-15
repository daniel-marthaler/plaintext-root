package ch.plaintext.email.persistence;

import ch.plaintext.email.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    List<Email> findByMandatOrderByCreatedAtDesc(String mandat);

    List<Email> findByMandatAndStatusOrderByCreatedAtDesc(String mandat, Email.EmailStatus status);

    List<Email> findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(Email.EmailStatus status, int maxRetries);

    List<Email> findByMandatAndStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            String mandat, Email.EmailStatus status, int maxRetries);

    List<Email> findByMandatAndDirectionOrderByCreatedAtDesc(String mandat, Email.EmailDirection direction);

    List<Email> findByMandatAndCreatedAtBetweenOrderByCreatedAtDesc(
            String mandat, LocalDateTime start, LocalDateTime end);

    long countByMandatAndStatus(String mandat, Email.EmailStatus status);
}
