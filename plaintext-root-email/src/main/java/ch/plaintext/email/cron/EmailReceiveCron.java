package ch.plaintext.email.cron;

import ch.plaintext.PlaintextCron;
import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.service.EmailReceiveService;
import ch.plaintext.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailReceiveCron implements PlaintextCron {

    private final EmailService emailService;
    private final EmailReceiveService emailReceiveService;

    @Override
    public String getDisplayName() {
        return "Email Empfang";
    }

    @Override
    public String getDefaultCronExpression() {
        // Every 10 minutes
        return "*/10 * * * *";
    }

    @Override
    public void run(String mandant) {
        log.info("Email receive cron started for mandate: {}", mandant);

        try {
            // Check if IMAP is configured for this mandate
            Optional<EmailConfig> config = emailService.getConfigForMandate(mandant);

            if (config.isEmpty() || !config.get().isImapConfigured()) {
                log.debug("IMAP not configured for mandate: {}, skipping email receive", mandant);
                return;
            }

            // Receive emails
            List<Email> receivedEmails = emailReceiveService.receiveEmails(mandant);

            log.info("Received {} new emails for mandate: {}", receivedEmails.size(), mandant);

        } catch (Exception e) {
            log.error("Email receive cron failed for mandate: {}", mandant, e);
        }
    }
}
