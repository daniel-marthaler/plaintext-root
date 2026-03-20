/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.web;

import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import ch.plaintext.discovery.service.DiscoveryEncryptionService;
import ch.plaintext.discovery.service.DiscoveryEncryptionService.DiscoveryToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryLoginControllerTest {

    @Mock
    private DiscoveryUserSessionRepository sessionRepository;

    @Mock
    private DiscoveryAppRepository appRepository;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private DiscoveryEncryptionService encryptionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession httpSession;

    private DiscoveryLoginController controller;

    @BeforeEach
    void setUp() {
        controller = new DiscoveryLoginController(
            sessionRepository, appRepository, userDetailsService,
            securityContextRepository, encryptionService, eventPublisher
        );
    }

    private void stubRequest() {
        when(request.getSession(true)).thenReturn(httpSession);
        when(request.getSession()).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn("session-123");
        when(request.getHeader("X-Forwarded-Proto")).thenReturn(null);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("X-Forwarded-Host")).thenReturn(null);
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getHeader("X-Forwarded-Port")).thenReturn(null);
    }

    @Nested
    class HandleDiscoveryLogin {

        @Test
        void handlesDbTokenSuccessfully() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppName("Test App");

            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setUserEmail("user@test.com");
            session.setApp(app);
            session.setTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
            session.setTokenUsed(false);

            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("db-token"))
                .thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<GrantedAuthority> auths = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_42"),
                new SimpleGrantedAuthority("PROPERTY_MANDAT_test")
            );
            UserDetails userDetails = new User("user@test.com", "pass", auths);
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
            stubRequest();

            ModelAndView result = controller.handleDiscoveryLogin("db-token", request, response);

            assertEquals("redirect:/index.html", result.getViewName());
            assertTrue(session.getTokenUsed());
            verify(sessionRepository).save(session);
        }

        @Test
        void rejectsExpiredDbToken() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppName("Test App");

            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setUserEmail("user@test.com");
            session.setApp(app);
            session.setTokenExpiresAt(LocalDateTime.now().minusMinutes(5));
            session.setTokenUsed(false);

            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("expired-token"))
                .thenReturn(Optional.of(session));

            ModelAndView result = controller.handleDiscoveryLogin("expired-token", request, response);

            assertEquals("redirect:/login.html?error=expired_discovery_token", result.getViewName());
        }

        @Test
        void fallsBackToEncryptedTokenWhenDbTokenNotFound() {
            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("encrypted-token"))
                .thenReturn(Optional.empty());

            DiscoveryToken discoveryToken = new DiscoveryToken(
                "user@test.com", System.currentTimeMillis(), "source-app", "nonce");
            when(encryptionService.decryptDiscoveryToken("encrypted-token")).thenReturn(discoveryToken);
            when(encryptionService.isTokenValid(discoveryToken)).thenReturn(true);

            DiscoveryApp sourceApp = new DiscoveryApp();
            sourceApp.setAppId("source-app");
            when(appRepository.findByAppId("source-app")).thenReturn(Optional.of(sourceApp));

            List<GrantedAuthority> auths = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_99"),
                new SimpleGrantedAuthority("PROPERTY_MANDAT_demo")
            );
            UserDetails userDetails = new User("user@test.com", "pass", auths);
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
            stubRequest();

            ModelAndView result = controller.handleDiscoveryLogin("encrypted-token", request, response);

            assertEquals("redirect:/index.html", result.getViewName());
        }

        @Test
        void rejectsInvalidEncryptedToken() {
            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("bad-token"))
                .thenReturn(Optional.empty());
            when(encryptionService.decryptDiscoveryToken("bad-token")).thenReturn(null);

            ModelAndView result = controller.handleDiscoveryLogin("bad-token", request, response);

            assertEquals("redirect:/login.html?error=invalid_discovery_token", result.getViewName());
        }

        @Test
        void rejectsExpiredEncryptedToken() {
            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("old-token"))
                .thenReturn(Optional.empty());

            DiscoveryToken expiredToken = new DiscoveryToken(
                "user@test.com", System.currentTimeMillis() - 600_000, "source-app", "nonce");
            when(encryptionService.decryptDiscoveryToken("old-token")).thenReturn(expiredToken);
            when(encryptionService.isTokenValid(expiredToken)).thenReturn(false);

            ModelAndView result = controller.handleDiscoveryLogin("old-token", request, response);

            assertEquals("redirect:/login.html?error=expired_discovery_token", result.getViewName());
        }

        @Test
        void rejectsTokenFromUnknownApp() {
            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("unknown-app-token"))
                .thenReturn(Optional.empty());

            DiscoveryToken token = new DiscoveryToken(
                "user@test.com", System.currentTimeMillis(), "unknown-app", "nonce");
            when(encryptionService.decryptDiscoveryToken("unknown-app-token")).thenReturn(token);
            when(encryptionService.isTokenValid(token)).thenReturn(true);
            when(appRepository.findByAppId("unknown-app")).thenReturn(Optional.empty());

            ModelAndView result = controller.handleDiscoveryLogin("unknown-app-token", request, response);

            assertEquals("redirect:/login.html?error=unknown_source_app", result.getViewName());
        }

        @Test
        void redirectsToLoginOnUserNotFound() {
            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("token"))
                .thenReturn(Optional.empty());

            DiscoveryToken token = new DiscoveryToken(
                "unknown@test.com", System.currentTimeMillis(), "source-app", "nonce");
            when(encryptionService.decryptDiscoveryToken("token")).thenReturn(token);
            when(encryptionService.isTokenValid(token)).thenReturn(true);

            DiscoveryApp sourceApp = new DiscoveryApp();
            sourceApp.setAppId("source-app");
            when(appRepository.findByAppId("source-app")).thenReturn(Optional.of(sourceApp));

            when(userDetailsService.loadUserByUsername("unknown@test.com"))
                .thenThrow(new UsernameNotFoundException("Not found"));

            ModelAndView result = controller.handleDiscoveryLogin("token", request, response);

            assertEquals("redirect:/login.html?error=user_not_found", result.getViewName());
        }

        @Test
        void handlesGeneralExceptionGracefully() {
            when(sessionRepository.findByLoginTokenAndTokenUsedFalse(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

            ModelAndView result = controller.handleDiscoveryLogin("token", request, response);

            assertEquals("redirect:/login.html?error=discovery_login_failed", result.getViewName());
        }

        @Test
        void extractsBaseUrlWithForwardedHeaders() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppName("Test App");

            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setUserEmail("user@test.com");
            session.setApp(app);
            session.setTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
            session.setTokenUsed(false);

            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("token"))
                .thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDetails userDetails = new User("user@test.com", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);

            when(request.getSession(true)).thenReturn(httpSession);
            when(request.getSession()).thenReturn(httpSession);
            when(httpSession.getId()).thenReturn("session-456");
            when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");
            when(request.getHeader("X-Forwarded-Host")).thenReturn("myapp.example.com");
            when(request.getServerPort()).thenReturn(443);
            when(request.getHeader("X-Forwarded-Port")).thenReturn("443");

            ModelAndView result = controller.handleDiscoveryLogin("token", request, response);

            assertEquals("redirect:/index.html", result.getViewName());
        }

        @Test
        void extractsUserIdWithoutAuthority() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppName("Test App");

            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setUserEmail("user@test.com");
            session.setApp(app);
            session.setTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
            session.setTokenUsed(false);

            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("token"))
                .thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // User with no PROPERTY_MYUSERID_ authority
            UserDetails userDetails = new User("user@test.com", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
            stubRequest();

            ModelAndView result = controller.handleDiscoveryLogin("token", request, response);

            assertEquals("redirect:/index.html", result.getViewName());
            // Should still succeed with userId = -1L
        }

        @Test
        void handlesNonDefaultPortInBaseUrl() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppName("Test App");

            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setUserEmail("user@test.com");
            session.setApp(app);
            session.setTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
            session.setTokenUsed(false);

            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("token"))
                .thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDetails userDetails = new User("user@test.com", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);

            when(request.getSession(true)).thenReturn(httpSession);
            when(request.getSession()).thenReturn(httpSession);
            when(httpSession.getId()).thenReturn("session-789");
            when(request.getHeader("X-Forwarded-Proto")).thenReturn(null);
            when(request.getScheme()).thenReturn("http");
            when(request.getHeader("X-Forwarded-Host")).thenReturn(null);
            when(request.getServerName()).thenReturn("localhost");
            when(request.getServerPort()).thenReturn(9090);
            when(request.getHeader("X-Forwarded-Port")).thenReturn(null);

            ModelAndView result = controller.handleDiscoveryLogin("token", request, response);

            assertEquals("redirect:/index.html", result.getViewName());
        }

        @Test
        void handlesDbTokenWithNullExpiration() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppName("Test App");

            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setUserEmail("user@test.com");
            session.setApp(app);
            session.setTokenExpiresAt(null); // null expiration => not expired
            session.setTokenUsed(false);

            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("token"))
                .thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDetails userDetails = new User("user@test.com", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
            stubRequest();

            ModelAndView result = controller.handleDiscoveryLogin("token", request, response);

            assertEquals("redirect:/index.html", result.getViewName());
        }

        @Test
        void handlesInvalidForwardedPort() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppName("Test App");

            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setUserEmail("user@test.com");
            session.setApp(app);
            session.setTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
            session.setTokenUsed(false);

            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("token"))
                .thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDetails userDetails = new User("user@test.com", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);

            when(request.getSession(true)).thenReturn(httpSession);
            when(request.getSession()).thenReturn(httpSession);
            when(httpSession.getId()).thenReturn("session-abc");
            when(request.getHeader("X-Forwarded-Proto")).thenReturn(null);
            when(request.getScheme()).thenReturn("http");
            when(request.getHeader("X-Forwarded-Host")).thenReturn(null);
            when(request.getServerName()).thenReturn("localhost");
            when(request.getServerPort()).thenReturn(80);
            when(request.getHeader("X-Forwarded-Port")).thenReturn("not-a-number");

            ModelAndView result = controller.handleDiscoveryLogin("token", request, response);

            assertEquals("redirect:/index.html", result.getViewName());
        }

        @Test
        void handlesUserIdParsingError() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppName("Test App");

            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setUserEmail("user@test.com");
            session.setApp(app);
            session.setTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
            session.setTokenUsed(false);

            when(sessionRepository.findByLoginTokenAndTokenUsedFalse("token"))
                .thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // User with malformed PROPERTY_MYUSERID_ authority
            UserDetails userDetails = new User("user@test.com", "pass", List.of(
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_not-a-number"),
                new SimpleGrantedAuthority("PROPERTY_MANDAT_test")
            ));
            when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
            stubRequest();

            ModelAndView result = controller.handleDiscoveryLogin("token", request, response);

            assertEquals("redirect:/index.html", result.getViewName());
        }
    }
}
