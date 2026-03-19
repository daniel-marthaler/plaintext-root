/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.service;

import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSendService {

    private final EmailService emailService;

    /**
     * Sends a test email using the given config to verify SMTP connectivity.
     */
    public void sendTestEmail(EmailConfig config, String testRecipient) throws Exception {
        log.info("Sending SMTP test email to {} using config '{}'", testRecipient, config.getConfigName());

        Session session = createSession(config);
        MimeMessage message = new MimeMessage(session);

        String fromAddress = config.getSmtpFromAddress();
        String fromName = config.getSmtpFromName();

        if (fromName != null && !fromName.isBlank()) {
            message.setFrom(new InternetAddress(fromAddress, fromName));
        } else {
            message.setFrom(new InternetAddress(fromAddress));
        }

        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(testRecipient));
        message.setSubject("Plaintext SMTP Test");
        message.setText("Dies ist eine automatische Test-Email von Plaintext.", "utf-8");

        Transport.send(message);
        log.info("SMTP test email sent successfully to {}", testRecipient);
    }

    public void sendEmail(Email email) {
        log.info("Attempting to send email ID: {} to: {}", email.getId(), email.getToAddress());

        try {
            // Use the config name stored in the email, or fall back to the first config for the mandate
            EmailConfig config;
            if (email.getConfigName() != null && !email.getConfigName().isBlank()) {
                config = emailService.getConfigByName(email.getMandat(), email.getConfigName())
                        .orElseThrow(() -> new IllegalStateException(
                                "No email configuration found for mandate: " + email.getMandat() +
                                " with config name: " + email.getConfigName()));
            } else {
                config = emailService.getConfigForMandate(email.getMandat())
                        .orElseThrow(() -> new IllegalStateException(
                                "No email configuration found for mandate: " + email.getMandat()));
            }

            if (!config.isSmtpConfigured()) {
                throw new IllegalStateException("SMTP is not configured for config: " +
                        config.getConfigName() + " (mandate: " + email.getMandat() + ")");
            }

            // Log configuration details
            logSmtpConfiguration(config);

            Session session = createSession(config);
            MimeMessage message = createMessage(session, email, config);

            Transport.send(message);

            emailService.markAsSent(email.getId(), message.getMessageID());
            log.info("Successfully sent email ID: {} using config: '{}'", email.getId(), config.getConfigName());

        } catch (Exception e) {
            log.error("Failed to send email ID: {}", email.getId(), e);
            emailService.markAsFailed(email.getId(), e.getMessage());
        }
    }

    private void logSmtpConfiguration(EmailConfig config) {
        String passwordMask = (config.getSmtpPassword() != null && !config.getSmtpPassword().isEmpty())
                ? "****" : "KEINES";

        log.info("Using SMTP configuration: '{}' | Host: {} | Port: {} | Username: {} | Password: {} | TLS: {} | SSL: {}",
                config.getConfigName(),
                config.getSmtpHost(),
                config.getSmtpPort(),
                config.getSmtpUsername() != null ? config.getSmtpUsername() : "KEINES",
                passwordMask,
                config.isSmtpUseTls(),
                config.isSmtpUseSsl());
    }

    private Session createSession(EmailConfig config) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", config.getSmtpPort());

        if (config.isSmtpUseTls()) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        if (config.isSmtpUseSsl()) {
            props.put("mail.smtp.ssl.enable", "true");
        }

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getSmtpUsername(), config.getSmtpPassword());
            }
        });
    }

    private MimeMessage createMessage(Session session, Email email, EmailConfig config) throws MessagingException {
        MimeMessage message = new MimeMessage(session);

        // Set from address
        String fromAddress = email.getFromAddress() != null ? email.getFromAddress() : config.getSmtpFromAddress();
        String fromName = config.getSmtpFromName();

        try {
            if (fromName != null && !fromName.isBlank()) {
                message.setFrom(new InternetAddress(fromAddress, fromName));
            } else {
                message.setFrom(new InternetAddress(fromAddress));
            }
        } catch (Exception e) {
            throw new MessagingException("Failed to set from address", e);
        }

        // Set recipients
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email.getToAddress()));

        if (email.getCcAddress() != null && !email.getCcAddress().isBlank()) {
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(email.getCcAddress()));
        }

        if (email.getBccAddress() != null && !email.getBccAddress().isBlank()) {
            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(email.getBccAddress()));
        }

        // Set subject and body
        message.setSubject(email.getSubject());

        if (email.isHtml()) {
            message.setContent(email.getBody(), "text/html; charset=utf-8");
        } else {
            message.setText(email.getBody(), "utf-8");
        }

        return message;
    }
}
