/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.cron;

import ch.plaintext.PlaintextCron;
import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailConfigRepository;
import ch.plaintext.email.service.EmailReceiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Per-mandant IMAP polling. Iterates over <i>every</i> IMAP-enabled email config
 * of the mandant and forwards new mails to all registered
 * {@link ch.plaintext.PlaintextIncomingEmailListener} beans via
 * {@link EmailReceiveService#receiveEmailsFromConfig(EmailConfig)}.
 *
 * <p>Disabled when {@code PLAINTEXT_ENV} is not {@code prod} so INT/dev
 * containers never compete with PROD for the same mailbox.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnExpression("'${PLAINTEXT_ENV:prod}' == 'prod'")
public class EmailReceiveCron implements PlaintextCron {

    private final EmailConfigRepository emailConfigRepository;
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
            List<EmailConfig> configs = emailConfigRepository.findByMandatAndImapEnabledTrue(mandant);
            if (configs.isEmpty()) {
                log.debug("No IMAP-enabled email configs for mandate: {}, skipping", mandant);
                return;
            }

            int total = 0;
            for (EmailConfig config : configs) {
                if (!config.isImapConfigured()) {
                    log.debug("Config '{}' (mandate: {}) is enabled but not properly configured, skipping",
                            config.getConfigName(), mandant);
                    continue;
                }
                try {
                    List<Email> received = emailReceiveService.receiveEmailsFromConfig(config);
                    total += received.size();
                } catch (Exception e) {
                    log.error("Failed to receive emails for config '{}' (mandate: {})",
                            config.getConfigName(), mandant, e);
                }
            }

            log.info("Received {} new emails for mandate: {} across {} config(s)",
                    total, mandant, configs.size());

        } catch (Exception e) {
            log.error("Email receive cron failed for mandate: {}", mandant, e);
        }
    }
}
