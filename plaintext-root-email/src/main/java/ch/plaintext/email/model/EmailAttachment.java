package ch.plaintext.email.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Entity representing an email attachment.
 * Attachments are stored with their metadata and binary content.
 *
 * @author info@plaintext.ch
 * @since 2024
 */
@Entity
@Table(name = "email_attachment")
@Data
@NoArgsConstructor
@ToString(exclude = {"email", "data"})
public class EmailAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    @JsonIgnore
    private Email email;

    @Column(name = "filename", nullable = false, length = 500)
    private String filename;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "data", nullable = false, columnDefinition = "BYTEA")
    @JsonIgnore
    private byte[] data;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
