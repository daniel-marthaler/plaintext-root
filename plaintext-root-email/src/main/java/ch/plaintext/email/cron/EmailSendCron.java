package ch.plaintext.email.cron;

import ch.plaintext.PlaintextCron;
import ch.plaintext.email.model.Email;
import ch.plaintext.email.service.EmailSendService;
import ch.plaintext.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailSendCron implements PlaintextCron {

    private final EmailService emailService;
    private final EmailSendService emailSendService;

    @Override
    public boolean isGlobal() {
        return true; // Global = mandanten-unabhängig, verarbeitet alle Email-Konfigurationen
    }

    @Override
    public String getDisplayName() {
        return "Email Versand (Global)";
    }

    @Override
    public String getDefaultCronExpression() {
        // Every 5 minutes
        return "*/5 * * * *";
    }

    @Override
    public void run(String mandant) {
        log.info("Email send cron started (global - processing all emails across all mandates)");

        try {
            // Get ALL queued emails regardless of mandate
            List<Email> queuedEmails = emailService.getQueuedEmails();

            log.info("Found {} queued emails to send across all mandates", queuedEmails.size());

            int successCount = 0;
            int failCount = 0;

            for (Email email : queuedEmails) {
                try {
                    String configInfo = email.getConfigName() != null && !email.getConfigName().isBlank()
                            ? "with config: '" + email.getConfigName() + "'"
                            : "with default config";
                    log.info("Sending email ID: {} for mandate: {} {}", email.getId(), email.getMandat(), configInfo);
                    emailSendService.sendEmail(email);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to send email ID: {} for mandate: {}", email.getId(), email.getMandat(), e);
                    failCount++;
                }
            }

            log.info("Email send cron completed. Sent {} emails successfully, {} failed.", successCount, failCount);

        } catch (Exception e) {
            log.error("Email send cron failed", e);
        }
    }
}
