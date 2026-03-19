/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "email")
@Data
@NoArgsConstructor
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mandat", nullable = false)
    private String mandat;

    @Column(name = "config_name")
    private String configName;

    @Column(name = "from_address", nullable = false, length = 1000)
    private String fromAddress;

    @Column(name = "to_address", nullable = false, length = 4000)
    private String toAddress;

    @Column(name = "cc_address", length = 4000)
    private String ccAddress;

    @Column(name = "bcc_address", length = 4000)
    private String bccAddress;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Column(name = "html", nullable = false)
    private boolean html = false;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailStatus status = EmailStatus.DRAFT;

    @Column(name = "direction", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailDirection direction = EmailDirection.OUTGOING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "max_retries")
    private int maxRetries = 3;

    @Column(name = "message_id")
    private String messageId;

    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EmailAttachment> attachments = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Helper method to add an attachment to this email
     */
    public void addAttachment(EmailAttachment attachment) {
        attachments.add(attachment);
        attachment.setEmail(this);
    }

    /**
     * Helper method to remove an attachment from this email
     */
    public void removeAttachment(EmailAttachment attachment) {
        attachments.remove(attachment);
        attachment.setEmail(null);
    }

    public enum EmailStatus {
        DRAFT,
        QUEUED,
        SENDING,
        SENT,
        FAILED,
        RECEIVED
    }

    public enum EmailDirection {
        INCOMING,
        OUTGOING
    }
}
