/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_config_v2",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mandat", "config_name"}))
@Data
@NoArgsConstructor
public class EmailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mandat", nullable = false)
    private String mandat;

    @Column(name = "config_name", nullable = false, length = 255)
    private String configName;

    // SMTP Configuration
    @Column(name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort = 587;

    @Column(name = "smtp_username")
    private String smtpUsername;

    @Column(name = "smtp_password")
    private String smtpPassword;

    @Column(name = "smtp_use_tls")
    private boolean smtpUseTls = true;

    @Column(name = "smtp_use_ssl")
    private boolean smtpUseSsl = false;

    @Column(name = "smtp_from_address")
    private String smtpFromAddress;

    @Column(name = "smtp_from_name")
    private String smtpFromName;

    @Column(name = "smtp_enabled")
    private boolean smtpEnabled = false;

    // IMAP Configuration
    @Column(name = "imap_enabled")
    private boolean imapEnabled = false;

    @Column(name = "imap_host")
    private String imapHost;

    @Column(name = "imap_port")
    private Integer imapPort = 993;

    @Column(name = "imap_username")
    private String imapUsername;

    @Column(name = "imap_password")
    private String imapPassword;

    @Column(name = "imap_use_ssl")
    private boolean imapUseSsl = true;

    @Column(name = "imap_folder")
    private String imapFolder = "INBOX";

    @Column(name = "imap_mark_as_read")
    private boolean imapMarkAsRead = true;

    @Column(name = "imap_delete_after_fetch")
    private boolean imapDeleteAfterFetch = false;

    @Column(name = "imap_poll_interval")
    private Integer imapPollInterval = 5; // in minutes - deprecated, managed by global cron

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @JsonIgnore
    public boolean isSmtpConfigured() {
        if (!smtpEnabled) return false;

        // Check required fields
        boolean hasRequiredFields = smtpHost != null && !smtpHost.isBlank() &&
                smtpUsername != null && !smtpUsername.isBlank() &&
                smtpFromAddress != null && !smtpFromAddress.isBlank();

        // For existing configs, password might be empty in form but exists in DB
        // For new configs (id == null), password must be provided
        boolean hasPassword = (id != null) || (smtpPassword != null && !smtpPassword.isBlank());

        return hasRequiredFields && hasPassword;
    }

    @JsonIgnore
    public boolean isImapConfigured() {
        if (!imapEnabled) return false;

        // Check required fields
        boolean hasRequiredFields = imapHost != null && !imapHost.isBlank() &&
                imapUsername != null && !imapUsername.isBlank();

        // For existing configs, password might be empty in form but exists in DB
        // For new configs (id == null), password must be provided
        boolean hasPassword = (id != null) || (imapPassword != null && !imapPassword.isBlank());

        return hasRequiredFields && hasPassword;
    }
}
