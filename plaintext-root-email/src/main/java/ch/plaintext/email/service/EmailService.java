/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.service;

import ch.plaintext.PlaintextEmailReceiver;
import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailConfigRepository;
import ch.plaintext.email.persistence.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService implements PlaintextEmailReceiver {

    private final EmailRepository emailRepository;
    private final EmailConfigRepository emailConfigRepository;

    @Transactional
    public Email createDraft(String mandat, String to, String subject, String body, boolean html) {
        return createDraft(mandat, to, null, null, subject, body, html);
    }

    @Transactional
    public Email createDraft(String mandat, String to, String cc, String bcc, String subject, String body, boolean html) {
        return createDraft(mandat, null, to, cc, bcc, subject, body, html);
    }

    @Transactional
    public Email createDraft(String mandat, String configName, String to, String cc, String bcc, String subject, String body, boolean html) {
        Email email = new Email();
        email.setMandat(mandat);
        email.setConfigName(configName); // Store the config name for later use during sending
        email.setToAddress(to);
        email.setCcAddress(cc);
        email.setBccAddress(bcc);
        email.setSubject(subject);
        email.setBody(body);
        email.setHtml(html);
        email.setStatus(Email.EmailStatus.DRAFT);
        email.setDirection(Email.EmailDirection.OUTGOING);

        // Set from address from config if available, otherwise use default
        Optional<EmailConfig> config;
        if (configName != null && !configName.isBlank()) {
            config = emailConfigRepository.findByMandatAndConfigName(mandat, configName);
        } else {
            config = emailConfigRepository.findFirstByMandatOrderByConfigNameAsc(mandat);
        }

        if (config.isPresent()) {
            email.setFromAddress(config.get().getSmtpFromAddress());
        } else {
            // Default fallback if no config exists
            email.setFromAddress("noreply@plaintext.ch");
            log.warn("No email config found for mandat {} and config {}, using default from address", mandat, configName);
        }

        return emailRepository.save(email);
    }

    @Transactional
    public Email queueEmail(Long emailId) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Email not found: " + emailId));

        if (email.getStatus() != Email.EmailStatus.DRAFT) {
            throw new IllegalStateException("Can only queue emails with status DRAFT");
        }

        email.setStatus(Email.EmailStatus.QUEUED);
        return emailRepository.save(email);
    }

    @Transactional
    public void markAsSent(Long emailId, String messageId) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Email not found: " + emailId));

        email.setStatus(Email.EmailStatus.SENT);
        email.setSentAt(LocalDateTime.now());
        email.setMessageId(messageId);
        emailRepository.save(email);
    }

    @Transactional
    public void markAsFailed(Long emailId, String errorMessage) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Email not found: " + emailId));

        email.setStatus(Email.EmailStatus.FAILED);
        email.setErrorMessage(errorMessage);
        email.setRetryCount(email.getRetryCount() + 1);
        emailRepository.save(email);
    }

    public List<Email> getEmailsForMandate(String mandat) {
        return emailRepository.findByMandatOrderByCreatedAtDesc(mandat);
    }

    public List<Email> getQueuedEmails() {
        return emailRepository.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                Email.EmailStatus.QUEUED, 3);
    }

    public List<Email> getQueuedEmailsForMandate(String mandat) {
        return emailRepository.findByMandatAndStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                mandat, Email.EmailStatus.QUEUED, 3);
    }

    /**
     * Gets the first email configuration for a mandate.
     * If multiple configurations exist, returns the first one ordered by config name.
     *
     * @deprecated Use getConfigByName() to specify which config to use.
     *             This method may return unexpected results if multiple configs exist.
     */
    @Deprecated
    public Optional<EmailConfig> getConfigForMandate(String mandat) {
        try {
            return emailConfigRepository.findFirstByMandatOrderByConfigNameAsc(mandat);
        } catch (Exception e) {
            log.error("Error getting config for mandate: {}. Multiple configs may exist - use getConfigByName() instead.", mandat, e);
            // Fallback: get first from list
            List<EmailConfig> configs = emailConfigRepository.findByMandatOrderByConfigNameAsc(mandat);
            if (configs.isEmpty()) {
                return Optional.empty();
            }
            log.warn("Multiple email configs found for mandate: {}. Using first one: {}. Consider using getConfigByName() instead.",
                    mandat, configs.get(0).getConfigName());
            return Optional.of(configs.get(0));
        }
    }

    public Optional<EmailConfig> getConfigByName(String mandat, String configName) {
        return emailConfigRepository.findByMandatAndConfigName(mandat, configName);
    }

    public List<EmailConfig> getConfigsForMandate(String mandat) {
        return emailConfigRepository.findByMandatOrderByConfigNameAsc(mandat);
    }

    public List<String> getConfigNamesForMandate(String mandat) {
        return emailConfigRepository.findByMandatOrderByConfigNameAsc(mandat)
                .stream()
                .map(EmailConfig::getConfigName)
                .toList();
    }

    public boolean configExists(String mandat, String configName) {
        return emailConfigRepository.existsByMandatAndConfigName(mandat, configName);
    }

    /**
     * Finds an email configuration by name across all mandates.
     * Useful for PlaintextEmailSender where we don't know the mandate upfront.
     *
     * @param configName the configuration name to search for
     * @return Optional containing the config if found, empty otherwise
     */
    public Optional<EmailConfig> findConfigByNameAcrossAllMandates(String configName) {
        return emailConfigRepository.findAll()
                .stream()
                .filter(config -> configName.equals(config.getConfigName()))
                .findFirst();
    }

    @Transactional
    public EmailConfig saveConfig(EmailConfig config) {
        // Validate config name
        if (config.getConfigName() == null || config.getConfigName().trim().isEmpty()) {
            throw new IllegalArgumentException("Config name cannot be empty");
        }

        // Trim and normalize the config name
        config.setConfigName(config.getConfigName().trim());

        // Check for duplicate name (only for new configs or when name has changed)
        if (config.getId() == null) {
            // New config - check if name already exists
            if (emailConfigRepository.existsByMandatAndConfigName(config.getMandat(), config.getConfigName())) {
                throw new IllegalArgumentException("Eine Konfiguration mit dem Namen '" + config.getConfigName() + "' existiert bereits für dieses Mandat");
            }
        } else {
            // Existing config - check if name has changed and if new name already exists
            Optional<EmailConfig> existing = emailConfigRepository.findById(config.getId());
            if (existing.isPresent()) {
                String oldName = existing.get().getConfigName();
                String newName = config.getConfigName();

                // If name changed, check if new name already exists
                if (!oldName.equals(newName)) {
                    if (emailConfigRepository.existsByMandatAndConfigName(config.getMandat(), newName)) {
                        throw new IllegalArgumentException("Eine Konfiguration mit dem Namen '" + newName + "' existiert bereits für dieses Mandat");
                    }
                }
            }
        }

        return emailConfigRepository.save(config);
    }

    @Transactional
    public void deleteConfig(Long configId) {
        emailConfigRepository.deleteById(configId);
    }

    @Transactional
    public Email updateDraft(Long emailId, String to, String cc, String subject, String body, boolean html) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Email not found: " + emailId));

        if (email.getStatus() != Email.EmailStatus.DRAFT) {
            throw new IllegalStateException("Can only update emails with status DRAFT");
        }

        // Update fields - mandate remains unchanged
        email.setToAddress(to);
        email.setCcAddress(cc);
        email.setSubject(subject);
        email.setBody(body);
        email.setHtml(html);

        log.debug("Updating email {} for mandate: {}", emailId, email.getMandat());
        return emailRepository.save(email);
    }

    @Transactional
    public void deleteEmail(Long emailId) {
        emailRepository.deleteById(emailId);
    }

    public long getQueuedCount(String mandat) {
        return emailRepository.countByMandatAndStatus(mandat, Email.EmailStatus.QUEUED);
    }

    public long getSentCount(String mandat) {
        return emailRepository.countByMandatAndStatus(mandat, Email.EmailStatus.SENT);
    }

    public long getFailedCount(String mandat) {
        return emailRepository.countByMandatAndStatus(mandat, Email.EmailStatus.FAILED);
    }

    public Optional<Email> findById(Long id) {
        return emailRepository.findById(id);
    }

    /**
     * Gibt die Anzahl der aktuell aktivierten IMAP E-Mail-Konfigurationen zurück.
     *
     * @return Anzahl der aktivierten IMAP Konfigurationen
     */
    public int getActiveImapConfigCount() {
        return (int) emailConfigRepository.findAll().stream()
                .filter(EmailConfig::isImapEnabled)
                .count();
    }

    // Implementation of PlaintextEmailReceiver interface

    @Override
    public Optional<Object> readEmail(Long emailId) {
        return emailRepository.findById(emailId).map(email -> (Object) email);
    }

    @Override
    public List<Object> readEmailsForMandate(String mandat) {
        return emailRepository.findByMandatOrderByCreatedAtDesc(mandat)
                .stream()
                .map(email -> (Object) email)
                .toList();
    }

    @Override
    public List<Object> readQueuedEmails() {
        return getQueuedEmails()
                .stream()
                .map(email -> (Object) email)
                .toList();
    }

    @Override
    public List<Object> readQueuedEmailsForMandate(String mandat) {
        return getQueuedEmailsForMandate(mandat)
                .stream()
                .map(email -> (Object) email)
                .toList();
    }

    @Override
    public List<Object> readIncomingEmailsForMandate(String mandat) {
        return emailRepository.findByMandatAndDirectionOrderByCreatedAtDesc(
                        mandat, Email.EmailDirection.INCOMING)
                .stream()
                .map(email -> (Object) email)
                .toList();
    }

    @Override
    public List<Object> readOutgoingEmailsForMandate(String mandat) {
        return emailRepository.findByMandatAndDirectionOrderByCreatedAtDesc(
                        mandat, Email.EmailDirection.OUTGOING)
                .stream()
                .map(email -> (Object) email)
                .toList();
    }
}
