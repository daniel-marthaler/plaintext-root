/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.service.EmailReceiveService;
import ch.plaintext.email.service.EmailSendService;
import ch.plaintext.email.service.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Email Configuration Backing Bean
 *
 * @author info@plaintext.ch
 * @since 2024
 */
@Component
@Named("emailConfigBackingBean")
@Data
@Slf4j
@Scope(scopeName = "session")
public class EmailConfigBackingBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String SMTP_TEST_RECIPIENT = "daniel@danielmarthaler.ch";

    @Autowired
    private transient PlaintextSecurity plaintextSecurity;

    @Autowired
    private transient EmailService emailService;

    @Autowired
    private transient EmailSendService emailSendService;

    @Autowired
    private transient EmailReceiveService emailReceiveService;

    private List<EmailConfig> configs = new ArrayList<>();
    private EmailConfig selectedConfig;

    private Map<Long, Boolean> imapTestResults = new HashMap<>();
    private Map<Long, String> imapTestMessages = new HashMap<>();
    private Map<Long, Boolean> smtpTestResults = new HashMap<>();
    private Map<Long, String> smtpTestMessages = new HashMap<>();

    @PostConstruct
    public void initThis() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null && fc.getPartialViewContext().isAjaxRequest()) {
            return;
        }
        loadConfigs();
    }

    public void loadConfigs() {
        if (plaintextSecurity == null) {
            log.warn("PlaintextSecurity is null, cannot load email configs");
            return;
        }

        String mandat = plaintextSecurity.getMandat();
        log.info("Loading email configurations for mandate: {}", mandat);

        configs.clear();
        configs.addAll(emailService.getConfigsForMandate(mandat));
        imapTestResults.clear();
        imapTestMessages.clear();
        smtpTestResults.clear();
        smtpTestMessages.clear();
        log.info("Loaded {} email configurations for mandate {}", configs.size(), mandat);
    }

    public void newConfig() {
        selectedConfig = new EmailConfig();
        selectedConfig.setMandat(getMandat());
        selectedConfig.setSmtpPort(587);
        selectedConfig.setSmtpUseTls(true);
        selectedConfig.setSmtpUseSsl(false);
        selectedConfig.setSmtpEnabled(false);
        selectedConfig.setImapPort(993);
        selectedConfig.setImapUseSsl(true);
        selectedConfig.setImapEnabled(false);
        selectedConfig.setImapFolder("INBOX");
        selectedConfig.setImapMarkAsRead(true);
        selectedConfig.setImapDeleteAfterFetch(false);

        log.debug("Created new email configuration");
    }

    public void selectConfig() {
        log.debug("SELECT Config called - selected: {}", selectedConfig != null ? selectedConfig.getId() : "null");
    }

    public void clearSelection() {
        log.debug("CLEAR SELECTION Config called");
        selectedConfig = null;
    }

    public void saveConfig() {
        FacesContext context = FacesContext.getCurrentInstance();

        log.info("saveConfig() called - selectedConfig is: {}", selectedConfig != null ? "NOT NULL" : "NULL");
        if (selectedConfig != null) {
            log.info("selectedConfig details - id: {}, configName: {}",
                    selectedConfig.getId(), selectedConfig.getConfigName());
        }

        if (selectedConfig == null) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Keine Konfiguration vorhanden."));
            log.error("saveConfig() - selectedConfig is NULL!");
            context.validationFailed();
            return;
        }

        // Validate config name
        if (selectedConfig.getConfigName() == null || selectedConfig.getConfigName().isBlank()) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                            "Bitte geben Sie einen Namen für die Konfiguration ein."));
            context.validationFailed();
            return;
        }

        try {
            selectedConfig = emailService.saveConfig(selectedConfig);

            // Note: Global cron job will automatically pick up changes on next run
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg",
                            "Email-Konfiguration wurde erfolgreich gespeichert."));

            log.info("Saved email configuration '{}' for mandate {}",
                    selectedConfig.getConfigName(), selectedConfig.getMandat());

            loadConfigs();
            clearSelection();

        } catch (Exception e) {
            log.error("Error saving email configuration", e);

            // Check if it's a constraint violation (duplicate name)
            if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                context.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                                "Eine Konfiguration mit diesem Namen existiert bereits für dieses Mandat."));
            } else {
                context.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                                "Konfiguration konnte nicht gespeichert werden: " + e.getMessage()));
            }
            context.validationFailed();
        }
    }

    public void deleteConfig() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (selectedConfig == null || selectedConfig.getId() == null) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Keine Konfiguration ausgewählt."));
            return;
        }

        try {
            emailService.deleteConfig(selectedConfig.getId());

            // Note: Global cron job will automatically skip deleted configs on next run
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg",
                            "Email-Konfiguration wurde gelöscht."));

            loadConfigs();
            clearSelection();

        } catch (Exception e) {
            log.error("Error deleting email configuration", e);
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                            "Konfiguration konnte nicht gelöscht werden: " + e.getMessage()));
        }
    }

    public void duplicateConfig() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (selectedConfig == null || selectedConfig.getId() == null) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Keine Konfiguration ausgewählt."));
            return;
        }

        String mandat = getMandat();
        String baseName = selectedConfig.getConfigName();

        // Remove existing suffix if present (e.g., "config_2" -> "config")
        String cleanBaseName = baseName.replaceAll("_\\d+$", "");

        // Try to save with incrementing suffix until we find a unique name
        int suffix = 2;
        int maxAttempts = 100; // Prevent infinite loop
        EmailConfig savedConfig = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String candidateName = cleanBaseName + "_" + suffix;

            try {
                // Create a copy of the selected config
                EmailConfig duplicate = new EmailConfig();
                duplicate.setConfigName(candidateName);
                duplicate.setMandat(mandat);

                // Copy SMTP settings
                duplicate.setSmtpHost(selectedConfig.getSmtpHost());
                duplicate.setSmtpPort(selectedConfig.getSmtpPort());
                duplicate.setSmtpUsername(selectedConfig.getSmtpUsername());
                duplicate.setSmtpPassword(selectedConfig.getSmtpPassword());
                duplicate.setSmtpFromAddress(selectedConfig.getSmtpFromAddress());
                duplicate.setSmtpFromName(selectedConfig.getSmtpFromName());
                duplicate.setSmtpUseTls(selectedConfig.isSmtpUseTls());
                duplicate.setSmtpUseSsl(selectedConfig.isSmtpUseSsl());
                duplicate.setSmtpEnabled(selectedConfig.isSmtpEnabled());

                // Copy IMAP settings
                duplicate.setImapHost(selectedConfig.getImapHost());
                duplicate.setImapPort(selectedConfig.getImapPort());
                duplicate.setImapUsername(selectedConfig.getImapUsername());
                duplicate.setImapPassword(selectedConfig.getImapPassword());
                duplicate.setImapUseSsl(selectedConfig.isImapUseSsl());
                duplicate.setImapFolder(selectedConfig.getImapFolder());
                duplicate.setImapMarkAsRead(selectedConfig.isImapMarkAsRead());
                duplicate.setImapDeleteAfterFetch(selectedConfig.isImapDeleteAfterFetch());
                duplicate.setImapEnabled(selectedConfig.isImapEnabled());

                // Try to save - will throw exception if name already exists
                savedConfig = emailService.saveConfig(duplicate);

                // Success!
                context.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg",
                                "Konfiguration wurde dupliziert als '" + candidateName + "'."));

                log.info("Duplicated email configuration '{}' to '{}'", baseName, candidateName);

                loadConfigs();
                clearSelection();
                return;

            } catch (Exception e) {
                // Check if it's a constraint violation
                if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                    // Name already exists, try next suffix
                    log.debug("Name '{}' already exists, trying next suffix", candidateName);
                    suffix++;
                } else {
                    // Different error, abort
                    log.error("Error duplicating email configuration", e);
                    context.addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                                    "Konfiguration konnte nicht dupliziert werden: " + e.getMessage()));
                    return;
                }
            }
        }

        // If we reach here, we exhausted all attempts
        context.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                        "Konnte keinen eindeutigen Namen finden nach " + maxAttempts + " Versuchen."));
    }

    public void testSmtpForConfig(Long configId) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            EmailConfig config = configs.stream()
                    .filter(c -> c.getId().equals(configId))
                    .findFirst().orElse(null);
            if (config == null) return;

            emailSendService.sendTestEmail(config, SMTP_TEST_RECIPIENT);
            smtpTestResults.put(configId, true);
            smtpTestMessages.put(configId, "Gesendet");
            log.info("SMTP test successful for config '{}'", config.getConfigName());
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "SMTP Test erfolgreich",
                            "Test-Mail wurde an " + SMTP_TEST_RECIPIENT + " gesendet."));
        } catch (Exception e) {
            smtpTestResults.put(configId, false);
            smtpTestMessages.put(configId, "Fehler");
            log.error("SMTP test failed for config ID {}", configId, e);
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "SMTP Test fehlgeschlagen", e.getMessage()));
        }
    }

    public void testImapForConfig(Long configId) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            EmailConfig config = configs.stream()
                    .filter(c -> c.getId().equals(configId))
                    .findFirst().orElse(null);
            if (config == null) return;

            int count = emailReceiveService.testImapConnection(config);
            imapTestResults.put(configId, true);
            imapTestMessages.put(configId, count + " Mails");
            log.info("IMAP test successful for config '{}': {} messages", config.getConfigName(), count);
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "IMAP Test erfolgreich",
                            count + " Mails in " + config.getImapFolder()));
        } catch (Exception e) {
            imapTestResults.put(configId, false);
            imapTestMessages.put(configId, "Fehler");
            log.error("IMAP test failed for config ID {}", configId, e);
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "IMAP Test fehlgeschlagen", e.getMessage()));
        }
    }

    public String getImapButtonLabel(Long configId) {
        if (configId == null || !imapTestResults.containsKey(configId)) return "Test";
        if (imapTestResults.get(configId)) return imapTestMessages.get(configId);
        return "Fehler";
    }

    public String getSmtpButtonLabel(Long configId) {
        if (configId == null || !smtpTestResults.containsKey(configId)) return "Test";
        if (smtpTestResults.get(configId)) return smtpTestMessages.get(configId);
        return "Fehler";
    }

    public String getImapTestStyleClass(Long configId) {
        if (configId == null || !imapTestResults.containsKey(configId)) return "ui-button-outlined";
        return imapTestResults.get(configId) ? "ui-button-success" : "ui-button-danger";
    }

    public String getSmtpTestStyleClass(Long configId) {
        if (configId == null || !smtpTestResults.containsKey(configId)) return "ui-button-outlined";
        return smtpTestResults.get(configId) ? "ui-button-success" : "ui-button-danger";
    }

    public boolean isSmtpConfigured() {
        return selectedConfig != null && selectedConfig.isSmtpConfigured();
    }

    public boolean isImapConfigured() {
        return selectedConfig != null && selectedConfig.isImapConfigured();
    }

    private String getMandat() {
        if (plaintextSecurity == null) {
            return "1";
        }
        return plaintextSecurity.getMandat();
    }

    public int getActiveCronJobCount() {
        return emailService.getActiveImapConfigCount();
    }
}
