/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security;

import ch.plaintext.boot.plugins.security.oidc.JdbcClientRegistrationRepository;
import ch.plaintext.boot.plugins.security.oidc.PlaintextOidcUserService;
import ch.plaintext.boot.plugins.security.service.MyRememberMeRepositoryRepository;
import ch.plaintext.boot.plugins.security.service.MyUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PlaintextSecurityConfig - security configuration beans.
 */
@ExtendWith(MockitoExtension.class)
class PlaintextSecurityConfigTest {

    @Mock
    private MyRememberMeRepositoryRepository tokenRepository;

    @Mock
    private MyUserDetailsService userDetailsService;

    @Mock
    private PlaintextAuthenticationSuccessHandler successHandler;

    @Mock
    private PlaintextSecurityProperties securityProperties;

    @Mock
    private JdbcClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private PlaintextOidcUserService oidcUserService;

    private PlaintextSecurityConfig createConfig() {
        return new PlaintextSecurityConfig(
                tokenRepository, userDetailsService, successHandler,
                securityProperties, clientRegistrationRepository, oidcUserService);
    }

    @Test
    void passwordEncoder_shouldReturnBCryptEncoder() {
        PlaintextSecurityConfig config = createConfig();

        PasswordEncoder encoder = config.passwordEncoder();

        assertNotNull(encoder);
        assertTrue(encoder instanceof BCryptPasswordEncoder);
    }

    @Test
    void passwordEncoder_shouldEncodeAndMatchPassword() {
        PlaintextSecurityConfig config = createConfig();

        PasswordEncoder encoder = config.passwordEncoder();
        String raw = "testPassword123";
        String encoded = encoder.encode(raw);

        assertTrue(encoder.matches(raw, encoded));
        assertFalse(encoder.matches("wrongPassword", encoded));
    }

    @Test
    void securityContextRepository_shouldReturnHttpSessionRepository() {
        PlaintextSecurityConfig config = createConfig();

        SecurityContextRepository repo = config.securityContextRepository();

        assertNotNull(repo);
    }

    @Test
    void rememberMeServices_shouldReturnService() {
        PlaintextSecurityConfig config = createConfig();

        assertNotNull(config.rememberMeServices());
    }
}
