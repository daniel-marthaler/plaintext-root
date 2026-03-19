/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.cron;

import ch.plaintext.PlaintextCron;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailConfigRepository;
import ch.plaintext.email.service.EmailReceiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Globaler Cron-Job zum Abholen von E-Mails für alle aktivierten IMAP-Konfigurationen.
 * Läuft alle 5 Minuten und prüft alle Konfigurationen mit aktiviertem IMAP.
 *
 * Implementiert das PlaintextCron Interface für Integration in das Cron-Management System.
 */
@Controller
@Scope("prototype")
@Slf4j
public class DynamicEmailReceiveCronService implements PlaintextCron {

    @Autowired
    private EmailConfigRepository emailConfigRepository;

    @Autowired
    private EmailReceiveService emailReceiveService;

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "E-Mail Empfang (Global)";
    }

    @Override
    public String getDefaultCronExpression() {
        // Alle 5 Minuten
        return "0 */5 * * *";
    }

    @Override
    public void run(String mandant) {
        log.debug("Starting global email receive cron job for mandant: {}", mandant);

        List<EmailConfig> imapConfigs = emailConfigRepository.findByImapEnabledTrue();

        if (imapConfigs.isEmpty()) {
            log.debug("No IMAP-enabled configurations found, skipping email receive");
            return;
        }

        log.info("Processing {} IMAP-enabled configurations", imapConfigs.size());

        for (EmailConfig config : imapConfigs) {
            if (!config.isImapConfigured()) {
                log.debug("Config '{}' (mandate: {}) is not properly configured, skipping",
                        config.getConfigName(), config.getMandat());
                continue;
            }

            try {
                log.debug("Receiving emails for config '{}' (mandate: {})",
                        config.getConfigName(), config.getMandat());
                emailReceiveService.receiveEmailsFromConfig(config);
            } catch (Exception e) {
                log.error("Error receiving emails for config '{}' (mandate: {}): {}",
                        config.getConfigName(), config.getMandat(), e.getMessage(), e);
            }
        }

        log.debug("Global email receive cron job completed for mandant: {}", mandant);
    }

    /**
     * Gibt die Anzahl der aktuell aktivierten E-Mail-Konfigurationen zurück.
     *
     * @return Anzahl der aktivierten Konfigurationen
     */
    public int getActiveConfigCount() {
        return emailConfigRepository.findByImapEnabledTrue().size();
    }
}
