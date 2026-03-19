/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailAttachment;
import ch.plaintext.email.service.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Email List Backing Bean
 *
 * @author info@plaintext.ch
 * @since 2024
 */
@Component
@Named("emailListBackingBean")
@Data
@Slf4j
@Scope(scopeName = "session")
public class EmailListBackingBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Autowired
    private PlaintextSecurity plaintextSecurity;

    @Autowired
    private EmailService emailService;

    private List<Email> emails = new ArrayList<>();
    private Email selectedEmail;
    private String filterStatus;

    @PostConstruct
    public void initThis() {
        loadEmails();
    }

    public void loadEmails() {
        if (plaintextSecurity == null) {
            log.warn("PlaintextSecurity is null, cannot load emails");
            return;
        }

        String mandat = plaintextSecurity.getMandat();
        log.info("Loading emails for mandate: {}", mandat);

        emails.clear();
        emails.addAll(emailService.getEmailsForMandate(mandat));
        log.info("Loaded {} emails for mandate {}", emails.size(), mandat);
    }

    public void newEmail() {
        selectedEmail = new Email();
        selectedEmail.setMandat(getMandat());
        selectedEmail.setStatus(Email.EmailStatus.DRAFT);
        selectedEmail.setDirection(Email.EmailDirection.OUTGOING);
        selectedEmail.setHtml(false);
        log.debug("Created new email");
    }

    public void selectEmail() {
        log.debug("SELECT Email called - selected: {}", selectedEmail != null ? selectedEmail.getId() : "null");
    }

    public void clearSelection() {
        log.debug("CLEAR SELECTION Email called");
        selectedEmail = null;
    }

    public void saveEmail() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (selectedEmail == null) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Keine Email ausgewählt."));
            return;
        }

        try {
            if (selectedEmail.getId() == null) {
                // New email - create draft
                Email saved = emailService.createDraft(
                        getMandat(),
                        selectedEmail.getToAddress(),
                        selectedEmail.getSubject(),
                        selectedEmail.getBody(),
                        selectedEmail.isHtml()
                );
                selectedEmail = saved;
                context.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Email-Entwurf wurde erstellt."));
            } else {
                // Update existing email (only if still DRAFT)
                if (selectedEmail.getStatus() == Email.EmailStatus.DRAFT) {
                    Email updated = emailService.updateDraft(
                            selectedEmail.getId(),
                            selectedEmail.getToAddress(),
                            selectedEmail.getCcAddress(),
                            selectedEmail.getSubject(),
                            selectedEmail.getBody(),
                            selectedEmail.isHtml()
                    );
                    selectedEmail = updated;
                    context.addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Email wurde aktualisiert."));
                } else {
                    context.addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_WARN, "Warnung",
                                    "Nur Entwürfe können bearbeitet werden."));
                }
            }

            loadEmails();
            clearSelection();

        } catch (Exception e) {
            log.error("Error saving email", e);
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Email konnte nicht gespeichert werden: " + e.getMessage()));
        }
    }

    public void queueEmail() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (selectedEmail == null || selectedEmail.getId() == null) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Keine Email ausgewählt."));
            return;
        }

        try {
            emailService.queueEmail(selectedEmail.getId());
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg",
                            "Email wurde zur Warteschlange hinzugefügt und wird versendet."));
            loadEmails();
            clearSelection();

        } catch (Exception e) {
            log.error("Error queueing email", e);
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                            "Email konnte nicht zur Warteschlange hinzugefügt werden: " + e.getMessage()));
        }
    }

    public void deleteEmail() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (selectedEmail == null || selectedEmail.getId() == null) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Keine Email ausgewählt."));
            return;
        }

        try {
            emailService.deleteEmail(selectedEmail.getId());
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Email wurde gelöscht."));
            loadEmails();
            clearSelection();

        } catch (Exception e) {
            log.error("Error deleting email", e);
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                            "Email konnte nicht gelöscht werden: " + e.getMessage()));
        }
    }

    public long getQueuedCount() {
        return emailService.getQueuedCount(getMandat());
    }

    public long getSentCount() {
        return emailService.getSentCount(getMandat());
    }

    public long getFailedCount() {
        return emailService.getFailedCount(getMandat());
    }

    private String getMandat() {
        if (plaintextSecurity == null) {
            return "1";
        }
        return plaintextSecurity.getMandat();
    }

    /**
     * Formatiert ein LocalDateTime für die Anzeige
     */
    public String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    /**
     * Download eines Email-Attachments
     */
    public void downloadAttachment(EmailAttachment attachment) {
        if (attachment == null || attachment.getData() == null) {
            log.warn("Cannot download attachment: attachment or data is null");
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();

        try {
            // Set response headers for file download
            externalContext.setResponseContentType(attachment.getContentType() != null
                ? attachment.getContentType()
                : "application/octet-stream");

            externalContext.setResponseContentLength(attachment.getData().length);

            // Encode filename for proper handling of special characters
            String encodedFilename = URLEncoder.encode(attachment.getFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");

            externalContext.setResponseHeader("Content-Disposition",
                "attachment; filename=\"" + attachment.getFilename() + "\"; filename*=UTF-8''" + encodedFilename);

            // Write attachment data to response
            OutputStream outputStream = externalContext.getResponseOutputStream();
            outputStream.write(attachment.getData());
            outputStream.flush();
            outputStream.close();

            facesContext.responseComplete();

            log.info("Downloaded attachment: {} ({} bytes)", attachment.getFilename(), attachment.getData().length);

        } catch (IOException e) {
            log.error("Error downloading attachment: {}", attachment.getFilename(), e);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                    "Attachment konnte nicht heruntergeladen werden: " + e.getMessage()));
        }
    }
}
