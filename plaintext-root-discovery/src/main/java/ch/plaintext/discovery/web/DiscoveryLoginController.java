/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.web;

import ch.plaintext.boot.plugins.security.PlaintextLoginEvent;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import ch.plaintext.discovery.service.DiscoveryEncryptionService;
import ch.plaintext.discovery.service.DiscoveryEncryptionService.DiscoveryToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Controller for cross-app login with temporary tokens.
 * Supports two token types:
 * 1. DB-based UUID tokens (from MQTT flow)
 * 2. RSA-encrypted tokens (from direct topbar navigation)
 */
@ConditionalOnProperty(value = "discovery.enabled", havingValue = "true", matchIfMissing = false)
@Controller
@RequestMapping("/discovery")
@Slf4j
@RequiredArgsConstructor
public class DiscoveryLoginController {

    private final DiscoveryUserSessionRepository sessionRepository;
    private final DiscoveryAppRepository appRepository;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final DiscoveryEncryptionService encryptionService;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping("/login")
    public ModelAndView handleDiscoveryLogin(@RequestParam("token") String token,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        try {
            log.info("Discovery login request received (token length: {})", token.length());

            // Try DB-based token first (MQTT flow)
            Optional<DiscoveryUserSession> sessionOpt =
                sessionRepository.findByLoginTokenAndTokenUsedFalse(token);

            if (sessionOpt.isPresent()) {
                log.info("Discovery login: DB-based token found");
                return handleDbToken(sessionOpt.get(), request, response);
            }

            // Try RSA-encrypted token (topbar direct navigation)
            log.info("Discovery login: trying RSA-encrypted token");
            return handleEncryptedToken(token, request, response);

        } catch (Exception e) {
            log.error("Error during discovery login", e);
            return new ModelAndView("redirect:/login.html?error=discovery_login_failed");
        }
    }

    private ModelAndView handleDbToken(DiscoveryUserSession session,
                                       HttpServletRequest request, HttpServletResponse response) {
        if (session.getTokenExpiresAt() != null &&
            session.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Expired discovery token for user: {}", session.getUserEmail());
            return new ModelAndView("redirect:/login.html?error=expired_discovery_token");
        }

        session.setTokenUsed(true);
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        return authenticateAndRedirect(session.getUserEmail(), session.getApp().getAppName(), request, response);
    }

    private ModelAndView handleEncryptedToken(String token,
                                              HttpServletRequest request, HttpServletResponse response) {
        DiscoveryToken discoveryToken = encryptionService.decryptDiscoveryToken(token);

        if (discoveryToken == null) {
            log.warn("Invalid discovery token (decryption failed)");
            return new ModelAndView("redirect:/login.html?error=invalid_discovery_token");
        }

        if (!encryptionService.isTokenValid(discoveryToken)) {
            log.warn("Expired discovery token for user: {} (age: {}ms)",
                discoveryToken.email(), System.currentTimeMillis() - discoveryToken.timestamp());
            return new ModelAndView("redirect:/login.html?error=expired_discovery_token");
        }

        // Verify source app is known
        var sourceApp = appRepository.findByAppId(discoveryToken.sourceAppId());
        if (sourceApp.isEmpty()) {
            log.warn("Discovery token from unknown app: {}", discoveryToken.sourceAppId());
            return new ModelAndView("redirect:/login.html?error=unknown_source_app");
        }

        log.info("Valid encrypted discovery token from app {} for user {}",
            discoveryToken.sourceAppId(), discoveryToken.email());

        return authenticateAndRedirect(discoveryToken.email(), discoveryToken.sourceAppId(), request, response);
    }

    private ModelAndView authenticateAndRedirect(String userEmail, String sourceAppName,
                                                 HttpServletRequest request, HttpServletResponse response) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authToken);
            SecurityContextHolder.setContext(context);

            // Ensure HTTP session exists before saving security context
            request.getSession(true);
            securityContextRepository.saveContext(context, request, response);

            log.info("Discovery auto-login successful for user: {} from app: {} (session: {})",
                userEmail, sourceAppName, request.getSession().getId());

            // Publish login event for discovery announcement
            publishLoginEvent(userEmail, userDetails, request);

            return new ModelAndView("redirect:/index.html");

        } catch (UsernameNotFoundException e) {
            log.warn("User {} not found locally for auto-login", userEmail);
            return new ModelAndView("redirect:/login.html?error=user_not_found");
        }
    }

    private void publishLoginEvent(String userEmail, UserDetails userDetails, HttpServletRequest request) {
        try {
            Long userId = extractUserId(userDetails);
            String mandat = extractMandat(userDetails);
            String baseUrl = extractBaseUrl(request);
            eventPublisher.publishEvent(new PlaintextLoginEvent(this, userEmail, userId, userEmail, mandat, baseUrl));
        } catch (Exception e) {
            log.warn("Failed to publish login event for discovery login: {}", e.getMessage());
        }
    }

    private Long extractUserId(UserDetails userDetails) {
        for (GrantedAuthority ga : userDetails.getAuthorities()) {
            String a = ga.getAuthority();
            if (a != null && a.startsWith("PROPERTY_MYUSERID_")) {
                try { return Long.parseLong(a.substring("PROPERTY_MYUSERID_".length())); }
                catch (NumberFormatException e) { /* ignore */ }
            }
        }
        return -1L;
    }

    private String extractMandat(UserDetails userDetails) {
        for (GrantedAuthority ga : userDetails.getAuthorities()) {
            String a = ga.getAuthority();
            if (a != null && a.startsWith("PROPERTY_MANDAT_")) {
                return a.substring("PROPERTY_MANDAT_".length()).toLowerCase();
            }
        }
        return "default";
    }

    private String extractBaseUrl(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) scheme = request.getScheme();
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) host = request.getServerName();
        int port = request.getServerPort();
        String forwardedPort = request.getHeader("X-Forwarded-Port");
        if (forwardedPort != null) {
            try { port = Integer.parseInt(forwardedPort); } catch (NumberFormatException e) { /* ignore */ }
        }
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + host + (defaultPort ? "" : ":" + port);
    }
}
