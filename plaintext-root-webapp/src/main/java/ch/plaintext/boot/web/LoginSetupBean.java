/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.web;

import ch.plaintext.oidc.entity.OidcConfig;
import ch.plaintext.oidc.service.OidcConfigService;
import ch.plaintext.settings.ISetupConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component("loginSetupBean")
@Slf4j
public class LoginSetupBean implements Serializable {

    @Autowired
    private ISetupConfigService setupConfigService;

    @Autowired(required = false)
    private OidcConfigService oidcConfigService;

    private static final String DEFAULT_MANDAT = "default";

    public boolean isOidcAutoRedirectActive() {
        try {
            if (!setupConfigService.isOidcAutoRedirectEnabled(DEFAULT_MANDAT)) {
                return false;
            }
            Long configId = setupConfigService.getOidcAutoRedirectConfigId(DEFAULT_MANDAT);
            if (configId == null || oidcConfigService == null) {
                return false;
            }
            return oidcConfigService.findAll().stream()
                    .anyMatch(c -> c.getId().equals(configId) && c.isEnabled());
        } catch (Exception e) {
            log.debug("Error checking OIDC auto-redirect: {}", e.getMessage());
            return false;
        }
    }

    public String getOidcAutoRedirectRegistrationId() {
        try {
            Long configId = setupConfigService.getOidcAutoRedirectConfigId(DEFAULT_MANDAT);
            if (configId == null || oidcConfigService == null) {
                return null;
            }
            return oidcConfigService.findAll().stream()
                    .filter(c -> c.getId().equals(configId) && c.isEnabled())
                    .map(OidcConfig::getRegistrationId)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Error getting OIDC redirect registration ID: {}", e.getMessage());
            return null;
        }
    }
}
