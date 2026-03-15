package ch.plaintext.email.cron;

import ch.plaintext.PlaintextCron;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailConfigRepository;
import ch.plaintext.email.service.EmailReceiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Test-Cron zum Empfangen von E-Mails.
 * Empfängt E-Mails für alle aktivierten IMAP-Konfigurationen (wie der globale E-Mail Empfangs-Cron).
 * Empfangene E-Mails werden automatisch vom EmailTestListener im Log ausgegeben
 * (PlaintextIncomingEmailListener wird automatisch benachrichtigt).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailReceiveTestCron implements PlaintextCron {

    private final EmailConfigRepository emailConfigRepository;
    private final EmailReceiveService emailReceiveService;

    @Override
    public boolean isGlobal() {
        return true; // Global = mandanten-unabhängig
    }

    @Override
    public String getDisplayName() {
        return "Email Test Empfang";
    }

    @Override
    public String getDefaultCronExpression() {
        // Deaktiviert per Default (null = nicht geplant)
        // Kann manuell in der Cron-Verwaltung aktiviert werden
        return null;
    }

    @Override
    public void run(String mandant) {
        log.info("Email receive test cron started - triggering email receive for all IMAP configurations");

        try {
            // Empfange E-Mails für alle aktivierten IMAP-Konfigurationen
            List<EmailConfig> imapConfigs = emailConfigRepository.findByImapEnabledTrue();

            if (imapConfigs.isEmpty()) {
                log.info("No IMAP-enabled configurations found, skipping email receive");
                return;
            }

            log.info("Processing {} IMAP-enabled configuration(s)", imapConfigs.size());

            int totalEmailsReceived = 0;
            for (EmailConfig config : imapConfigs) {
                if (!config.isImapConfigured()) {
                    log.debug("Config '{}' (mandate: {}) is not properly configured, skipping",
                            config.getConfigName(), config.getMandat());
                    continue;
                }

                try {
                    log.info("Receiving emails for config '{}' (mandate: {})",
                            config.getConfigName(), config.getMandat());
                    int received = emailReceiveService.receiveEmailsFromConfig(config).size();
                    totalEmailsReceived += received;
                    log.info("Received {} email(s) for config '{}'", received, config.getConfigName());
                } catch (Exception e) {
                    log.error("Error receiving emails for config '{}' (mandate: {}): {}",
                            config.getConfigName(), config.getMandat(), e.getMessage(), e);
                }
            }

            log.info("Email receive test cron completed - received {} total email(s). " +
                    "EmailTestListener was automatically notified for 'maintenance' config emails.", totalEmailsReceived);

        } catch (Exception e) {
            log.error("Email receive test cron failed", e);
        }
    }
}
