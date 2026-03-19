/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "plaintext.security")
@Data
public class PlaintextSecurityProperties {

    /**
     * Zusätzliche Pfade für die CSRF wird ignoriert (ergänzend zu Framework-Defaults).
     */
    private List<String> csrfIgnorePatterns = new ArrayList<>();

    /**
     * Zusätzliche Pfade die ohne Authentication erreichbar sind (ergänzend zu Framework-Defaults).
     */
    private List<String> permitAllPatterns = new ArrayList<>();
}
