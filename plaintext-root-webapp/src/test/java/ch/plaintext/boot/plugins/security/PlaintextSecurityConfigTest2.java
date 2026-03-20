/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security;

import ch.plaintext.boot.plugins.security.service.MyRememberMeRepositoryRepository;
import ch.plaintext.boot.plugins.security.service.MyUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Extended tests for PlaintextSecurityConfig - verifying bean creation.
 */
@ExtendWith(MockitoExtension.class)
class PlaintextSecurityConfigTest2 {

    @Mock
    private MyRememberMeRepositoryRepository tokenRepository;

    @Mock
    private MyUserDetailsService userDetail;

    @Mock
    private PlaintextAuthenticationSuccessHandler authenticationSuccessHandler;

    @Mock
    private PlaintextSecurityProperties securityProperties;

    @Test
    void passwordEncoder_shouldReturnBCryptEncoder() {
        when(securityProperties.getCsrfIgnorePatterns()).thenReturn(new ArrayList<>());
        when(securityProperties.getPermitAllPatterns()).thenReturn(new ArrayList<>());

        PlaintextSecurityConfig config = new PlaintextSecurityConfig(
                tokenRepository, userDetail, authenticationSuccessHandler, securityProperties);

        PasswordEncoder encoder = config.passwordEncoder();

        assertNotNull(encoder);
        String encoded = encoder.encode("test");
        assertTrue(encoded.startsWith("$2a$"));
    }

    @Test
    void authenticationManager_shouldNotThrow() throws Exception {
        when(securityProperties.getCsrfIgnorePatterns()).thenReturn(new ArrayList<>());
        when(securityProperties.getPermitAllPatterns()).thenReturn(new ArrayList<>());

        PlaintextSecurityConfig config = new PlaintextSecurityConfig(
                tokenRepository, userDetail, authenticationSuccessHandler, securityProperties);

        AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
        AuthenticationManager mockManager = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(mockManager);

        AuthenticationManager manager = config.authenticationManager(authConfig);

        assertNotNull(manager);
        assertSame(mockManager, manager);
    }

    @Test
    void securityContextRepository_shouldReturnInstance() {
        when(securityProperties.getCsrfIgnorePatterns()).thenReturn(new ArrayList<>());
        when(securityProperties.getPermitAllPatterns()).thenReturn(new ArrayList<>());

        PlaintextSecurityConfig config = new PlaintextSecurityConfig(
                tokenRepository, userDetail, authenticationSuccessHandler, securityProperties);

        SecurityContextRepository repo = config.securityContextRepository();

        assertNotNull(repo);
    }

    @Test
    void rememberMeServices_shouldReturnInstance() {
        when(securityProperties.getCsrfIgnorePatterns()).thenReturn(new ArrayList<>());
        when(securityProperties.getPermitAllPatterns()).thenReturn(new ArrayList<>());

        PlaintextSecurityConfig config = new PlaintextSecurityConfig(
                tokenRepository, userDetail, authenticationSuccessHandler, securityProperties);

        PersistentTokenBasedRememberMeServices services = config.rememberMeServices();

        assertNotNull(services);
    }

    @Test
    void rememberMeFilter_shouldReturnInstance() {
        when(securityProperties.getCsrfIgnorePatterns()).thenReturn(new ArrayList<>());
        when(securityProperties.getPermitAllPatterns()).thenReturn(new ArrayList<>());

        PlaintextSecurityConfig config = new PlaintextSecurityConfig(
                tokenRepository, userDetail, authenticationSuccessHandler, securityProperties);

        PersistentTokenBasedRememberMeServices services = config.rememberMeServices();
        AuthenticationManager authManager = mock(AuthenticationManager.class);

        var filter = config.rememberMeFilter(services, authManager);

        assertNotNull(filter);
    }
}
