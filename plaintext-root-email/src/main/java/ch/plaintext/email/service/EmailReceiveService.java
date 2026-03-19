/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.service;

import ch.plaintext.PlaintextIncomingEmailListener;
import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailAttachment;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailRepository;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailReceiveService {

    private final EmailService emailService;
    private final EmailRepository emailRepository;
    private final List<PlaintextIncomingEmailListener> emailListeners;

    public List<Email> receiveEmails(String mandat) {
        log.debug("Attempting to receive emails for mandate: {}", mandat);

        List<Email> receivedEmails = new ArrayList<>();

        try {
            var configOptional = emailService.getConfigForMandate(mandat);

            if (configOptional.isEmpty()) {
                log.warn("No email configuration found for mandate: {} - skipping email receive", mandat);
                return receivedEmails;
            }

            EmailConfig config = configOptional.get();
            receivedEmails.addAll(receiveEmailsFromConfig(config));

        } catch (Exception e) {
            log.error("Failed to receive emails for mandate: {}", mandat, e);
        }

        return receivedEmails;
    }

    /**
     * Tests IMAP connection and returns the total message count in the configured folder.
     */
    public int testImapConnection(EmailConfig config) throws MessagingException {
        Store store = connectToImapStore(config);
        Folder folder = store.getFolder(config.getImapFolder() != null ? config.getImapFolder() : "INBOX");
        folder.open(Folder.READ_ONLY);
        int messageCount = folder.getMessageCount();
        folder.close(false);
        store.close();
        return messageCount;
    }

    public List<Email> receiveEmailsFromConfig(EmailConfig config) {
        log.info("Attempting to receive emails for config: {} (mandate: {})",
                config.getConfigName(), config.getMandat());

        List<Email> receivedEmails = new ArrayList<>();

        try {
            if (!config.isImapConfigured()) {
                log.warn("IMAP is not configured or not enabled for config: {}", config.getConfigName());
                return receivedEmails;
            }

            Store store = connectToImapStore(config);
            Folder folder = store.getFolder(config.getImapFolder());
            folder.open(Folder.READ_WRITE);

            // Only fetch UNSEEN (unread) messages instead of all messages
            Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            log.info("Found {} unread messages in folder: {} for config: {}",
                    messages.length, config.getImapFolder(), config.getConfigName());

            for (Message message : messages) {
                try {
                    Email email = convertToEmail(message, config.getMandat());
                    Email savedEmail = emailRepository.save(email);
                    receivedEmails.add(savedEmail);

                    // Notify listeners about new email
                    notifyListeners(savedEmail, config.getConfigName());

                    // Handle flags based on configuration
                    if (config.isImapMarkAsRead()) {
                        message.setFlag(Flags.Flag.SEEN, true);
                        log.debug("Marked message as read (SEEN flag set)");
                    } else {
                        // Ensure message stays unread
                        message.setFlag(Flags.Flag.SEEN, false);
                        log.debug("Keeping message as unread (SEEN flag remains false)");
                    }

                    if (config.isImapDeleteAfterFetch()) {
                        message.setFlag(Flags.Flag.DELETED, true);
                        log.debug("Marked message for deletion");
                    }

                } catch (Exception e) {
                    log.error("Failed to process message", e);
                }
            }

            folder.close(true); // expunge deleted messages
            store.close();

            log.info("Successfully received {} emails for config: {}",
                    receivedEmails.size(), config.getConfigName());

        } catch (Exception e) {
            log.error("Failed to receive emails for config: {}", config.getConfigName(), e);
        }

        return receivedEmails;
    }

    private Store connectToImapStore(EmailConfig config) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", config.getImapHost());
        props.put("mail.imaps.port", config.getImapPort());

        if (config.isImapUseSsl()) {
            props.put("mail.imaps.ssl.enable", "true");
        }

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(config.getImapHost(), config.getImapUsername(), config.getImapPassword());

        return store;
    }

    private Email convertToEmail(Message message, String mandat) throws Exception {
        Email email = new Email();
        email.setMandat(mandat);
        email.setDirection(Email.EmailDirection.INCOMING);
        email.setStatus(Email.EmailStatus.RECEIVED);

        // From address
        if (message.getFrom() != null && message.getFrom().length > 0) {
            email.setFromAddress(message.getFrom()[0].toString());
        }

        // To address
        if (message.getRecipients(Message.RecipientType.TO) != null) {
            email.setToAddress(InternetAddressArrayToString(message.getRecipients(Message.RecipientType.TO)));
        }

        // CC address
        if (message.getRecipients(Message.RecipientType.CC) != null) {
            email.setCcAddress(InternetAddressArrayToString(message.getRecipients(Message.RecipientType.CC)));
        }

        // Subject
        email.setSubject(message.getSubject());

        // Body and attachments
        processMessageContent(message, email);

        // Message ID
        String[] messageIds = message.getHeader("Message-ID");
        if (messageIds != null && messageIds.length > 0) {
            email.setMessageId(messageIds[0]);
        }

        // Received date
        if (message.getReceivedDate() != null) {
            email.setReceivedAt(LocalDateTime.ofInstant(
                    message.getReceivedDate().toInstant(), ZoneId.systemDefault()));
        } else {
            email.setReceivedAt(LocalDateTime.now());
        }

        return email;
    }

    /**
     * Process message content - extract both body text and attachments
     */
    private void processMessageContent(Message message, Email email) throws Exception {
        StringBuilder bodyBuilder = new StringBuilder();
        boolean hasHtml = false;

        if (message.isMimeType("text/plain")) {
            bodyBuilder.append(message.getContent().toString());
        } else if (message.isMimeType("text/html")) {
            bodyBuilder.append(message.getContent().toString());
            hasHtml = true;
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            hasHtml = processMultipart(mimeMultipart, bodyBuilder, email);
        }

        email.setBody(bodyBuilder.toString());
        email.setHtml(hasHtml);
    }

    /**
     * Process multipart content - extract body text and attachments
     * @return true if HTML content was found
     */
    private boolean processMultipart(MimeMultipart mimeMultipart, StringBuilder bodyBuilder, Email email) throws Exception {
        boolean hasHtml = false;
        int count = mimeMultipart.getCount();

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);

            if (bodyPart.isMimeType("text/plain")) {
                bodyBuilder.append(bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                bodyBuilder.append(bodyPart.getContent());
                hasHtml = true;
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                // Recursively process nested multipart
                if (processMultipart((MimeMultipart) bodyPart.getContent(), bodyBuilder, email)) {
                    hasHtml = true;
                }
            } else if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                       (bodyPart.getFileName() != null && !bodyPart.getFileName().isEmpty())) {
                // This is an attachment
                processAttachment(bodyPart, email);
            }
        }

        return hasHtml;
    }

    /**
     * Process and save an attachment
     */
    private void processAttachment(BodyPart bodyPart, Email email) {
        try {
            String filename = bodyPart.getFileName();
            String contentType = bodyPart.getContentType();

            // Read attachment data
            InputStream is = bodyPart.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] data = baos.toByteArray();

            // Create attachment entity
            EmailAttachment attachment = new EmailAttachment();
            attachment.setFilename(filename);
            attachment.setContentType(contentType);
            attachment.setSizeBytes((long) data.length);
            attachment.setData(data);

            // Add to email
            email.addAttachment(attachment);

            log.debug("Processed attachment: {} ({} bytes)", filename, data.length);

        } catch (Exception e) {
            log.error("Failed to process attachment", e);
        }
    }

    private String InternetAddressArrayToString(Address[] addresses) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addresses.length; i++) {
            sb.append(addresses[i].toString());
            if (i < addresses.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private void notifyListeners(Email email, String configName) {
        if (emailListeners == null || emailListeners.isEmpty()) {
            log.debug("No email listeners registered");
            return;
        }

        log.debug("Notifying email listeners about email {} from config {}",
                email.getId(), configName);

        for (PlaintextIncomingEmailListener listener : emailListeners) {
            try {
                // Check if listener is interested in this config
                List<String> interestedConfigs = listener.getConfigNamesToListenTo();
                if (interestedConfigs != null && !interestedConfigs.isEmpty() &&
                        !interestedConfigs.contains(configName)) {
                    log.debug("Listener {} not interested in config {}, skipping",
                            listener.getListenerName(), configName);
                    continue;
                }

                log.debug("Notifying listener: {} for config: {}",
                        listener.getListenerName(), configName);
                listener.onEmailReceived(email.getId(), email.getMandat(), configName);
            } catch (Exception e) {
                log.error("Failed to notify listener: {}", listener.getListenerName(), e);
            }
        }
    }
}
