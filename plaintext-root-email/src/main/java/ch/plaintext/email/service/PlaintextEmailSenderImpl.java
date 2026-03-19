/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.service;

import ch.plaintext.PlaintextEmailSender;
import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of PlaintextEmailSender interface.
 * Creates email drafts and immediately queues them for delivery by the email cron job.
 *
 * @author info@plaintext.ch
 * @since 2024
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlaintextEmailSenderImpl implements PlaintextEmailSender {

    private final EmailService emailService;

    @Override
    @Transactional
    public Long sendEmail(String configName, String to, String subject, String body, boolean html) {
        return sendEmail(configName, to, null, null, subject, body, html);
    }

    @Override
    @Transactional
    public Long sendEmail(String configName, String to, String cc, String bcc, String subject, String body, boolean html) {
        log.info("📧 PlaintextEmailSender: Queueing email using config '{}' to: {}", configName, to);

        if (configName == null || configName.trim().isEmpty()) {
            String errorMsg = "❌ EMAIL CONFIG ERROR: Config name is required but was null or empty";
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (to == null || to.trim().isEmpty()) {
            String errorMsg = "❌ EMAIL CONFIG ERROR: Recipient (to) is required but was null or empty";
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        try {
            // Zuerst prüfen ob die Config existiert und detailliertes Logging
            Optional<EmailConfig> configOpt = emailService.findConfigByNameAcrossAllMandates(configName);

            if (configOpt.isEmpty()) {
                log.warn("⚠️ EMAIL CONFIG NOT FOUND: No email configuration with name '{}' exists in the system. " +
                    "Skipping email send. Please create this configuration in /emailconfig.html before sending emails.",
                    configName);
                log.warn("📋 HELP: To fix this:");
                log.warn("   1. Go to http://localhost:8080/emailconfig.html");
                log.warn("   2. Create a new email configuration with name: '{}'", configName);
                log.warn("   3. Configure SMTP settings (host, port, credentials)");
                log.warn("   4. Save the configuration");
                return null; // Return null to indicate email was not sent
            }

            EmailConfig config = configOpt.get();
            String mandat = config.getMandat();

            log.info("✅ Email config '{}' found for mandate: {}", configName, mandat);
            log.debug("   SMTP Host: {}, Port: {}, From: {}",
                config.getSmtpHost(), config.getSmtpPort(), config.getSmtpFromAddress());

            // Create draft with all recipients and specific config
            Email email = emailService.createDraft(mandat, configName, to, cc, bcc, subject, body, html);

            // Queue for sending
            Email queuedEmail = emailService.queueEmail(email.getId());

            log.info("✅ Email queued successfully with ID: {} using config: '{}' (mandate: {})",
                    queuedEmail.getId(), configName, mandat);
            return queuedEmail.getId();

        } catch (IllegalStateException e) {
            // Already logged above, just rethrow
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format(
                "❌ EMAIL SEND FAILED: Unexpected error while queueing email using config '%s' to '%s': %s",
                configName, to, e.getMessage()
            );
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }
}
