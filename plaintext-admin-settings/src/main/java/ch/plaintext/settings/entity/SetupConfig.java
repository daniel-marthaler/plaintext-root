/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.settings.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "SETUP_CONFIG")
@Data
@EntityListeners(AuditingEntityListener.class)
public class SetupConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String mandat;

    @Column(name = "AUTOLOGIN_ENABLED", nullable = false)
    private boolean autologinEnabled = false;

    @Column(name = "OIDC_AUTO_REDIRECT_ENABLED", nullable = false)
    private boolean oidcAutoRedirectEnabled = false;

    @Column(name = "OIDC_AUTO_REDIRECT_CONFIG_ID")
    private Long oidcAutoRedirectConfigId;

    @Column(name = "PASSWORD_MANAGEMENT_ENABLED", nullable = false)
    private boolean passwordManagementEnabled = true;

    @Column(name = "ROOT_USER_ENABLED", nullable = false)
    private boolean rootUserEnabled = true;

    @CreatedBy
    @Column(name = "CREATED_BY")
    private String createdBy;

    @CreatedDate
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "LAST_MODIFIED_BY")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}
