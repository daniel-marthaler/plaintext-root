/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.anforderungen.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

/**
 * API settings for Claude automation - per mandat
 * Each mandat has its own settings instance
 */
@Entity
@Table(name = "anforderung_api_settings", indexes = {
    @Index(name = "idx_api_settings_mandat", columnList = "mandat", unique = true)
})
@Data
public class AnforderungApiSettings implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mandat", nullable = false, length = 100, unique = true)
    private String mandat;

    @Column(name = "api_token", length = 500)
    private String apiToken;

    @Column(name = "claude_automation_enabled")
    private Boolean claudeAutomationEnabled;
}
