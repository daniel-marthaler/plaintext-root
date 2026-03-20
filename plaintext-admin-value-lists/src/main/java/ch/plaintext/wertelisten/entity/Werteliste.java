/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.wertelisten.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "werteliste", indexes = {
    @Index(name = "idx_werteliste_mandat", columnList = "mandat"),
    @Index(name = "idx_werteliste_key_mandat", columnList = "werte_key, mandat", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(WertelisteId.class)
public class Werteliste {

    @Id
    @Column(name = "werte_key", nullable = false, length = 255)
    private String key;

    @Id
    @Column(name = "mandat", nullable = false, length = 100)
    private String mandat;

    @OneToMany(mappedBy = "werteliste", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<WertelisteEntry> entries = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @CreatedBy
    @Column(name = "created_by", length = 255, updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    public void addEntry(WertelisteEntry entry) {
        entries.add(entry);
        entry.setWerteliste(this);
    }

    public void removeEntry(WertelisteEntry entry) {
        entries.remove(entry);
        entry.setWerteliste(null);
    }
}
