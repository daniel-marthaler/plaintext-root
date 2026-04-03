/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security.oidc;

import ch.plaintext.oidc.entity.OidcConfig;
import ch.plaintext.oidc.service.OidcConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class JdbcClientRegistrationRepository implements ClientRegistrationRepository {

    private final OidcConfigService oidcConfigService;

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Optional<OidcConfig> configOpt = oidcConfigService.getActiveConfig();
        if (configOpt.isEmpty()) {
            log.debug("No active OIDC config found for registrationId: {}", registrationId);
            return null;
        }

        OidcConfig config = configOpt.get();
        String expectedId = toRegistrationId(config);
        if (!expectedId.equals(registrationId)) {
            log.debug("Registration ID mismatch: expected={}, requested={}", expectedId, registrationId);
            return null;
        }

        return buildClientRegistration(config);
    }

    public Optional<ClientRegistration> getActiveRegistration() {
        return oidcConfigService.getActiveConfig().map(this::buildClientRegistration);
    }

    private ClientRegistration buildClientRegistration(OidcConfig config) {
        String issuer = config.getIssuerUrl().replaceAll("/+$", "");

        String[] scopes = config.getScopes() != null
                ? config.getScopes().split(",")
                : new String[]{"openid", "profile", "email"};

        ClientAuthenticationMethod authMethod = (config.getClientSecret() == null || config.getClientSecret().isBlank())
                ? ClientAuthenticationMethod.NONE
                : ClientAuthenticationMethod.CLIENT_SECRET_BASIC;

        return ClientRegistration.withRegistrationId(toRegistrationId(config))
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret() != null ? config.getClientSecret() : "")
                .clientAuthenticationMethod(authMethod)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope(Arrays.stream(scopes).map(String::trim).toArray(String[]::new))
                .authorizationUri(issuer + "/protocol/openid-connect/auth")
                .tokenUri(issuer + "/protocol/openid-connect/token")
                .userInfoUri(issuer + "/protocol/openid-connect/userinfo")
                .jwkSetUri(issuer + "/protocol/openid-connect/certs")
                .issuerUri(issuer)
                .userNameAttributeName("preferred_username")
                .clientName(config.getName())
                .build();
    }

    static String toRegistrationId(OidcConfig config) {
        return "keycloak";
    }
}
