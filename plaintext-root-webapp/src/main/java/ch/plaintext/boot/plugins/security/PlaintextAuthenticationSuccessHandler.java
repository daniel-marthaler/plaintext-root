/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlaintextAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {

        String userEmail = authentication.getName();
        // For OIDC users: use email from token
        if (authentication instanceof OAuth2AuthenticationToken && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            if (oidcUser.getEmail() != null && !oidcUser.getEmail().isBlank()) {
                userEmail = oidcUser.getEmail();
            }
        }
        Long userId = extractUserId(authentication);
        String mandat = extractMandat(authentication);

        try {
            String baseUrl = extractBaseUrl(request);
            eventPublisher.publishEvent(new PlaintextLoginEvent(this, userEmail, userId, userEmail, mandat, baseUrl));
            log.debug("Published PlaintextLoginEvent for user: {} baseUrl: {}", userEmail, baseUrl);
        } catch (Exception e) {
            log.warn("Failed to publish login event: {}", e.getMessage());
        }

        String contextPath = request.getContextPath();
        String redirectUrl;
        if (contextPath != null && !contextPath.isEmpty()) {
            redirectUrl = contextPath + "/index.html";
        } else {
            redirectUrl = "index.html";
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities != null && !authorities.isEmpty()) {
            for (GrantedAuthority authority : authorities) {
                String authStr = authority.getAuthority();
                if (authStr != null && authStr.startsWith("PROPERTY_STARTPAGE_")) {
                    redirectUrl = authStr.substring("PROPERTY_STARTPAGE_".length());
                    log.info("User {} has startpage configured: {}", userEmail, redirectUrl);
                    break;
                }
            }
        }

        // Ensure absolute path for redirect (relative would resolve against /login/oauth2/code/)
        if (!redirectUrl.startsWith("/") && !redirectUrl.startsWith("http")) {
            redirectUrl = "/" + redirectUrl;
        }
        log.debug("Redirecting user {} to {}", userEmail, redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private Long extractUserId(Authentication auth) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if (a != null && a.startsWith("PROPERTY_MYUSERID_")) {
                try {
                    return Long.parseLong(a.substring("PROPERTY_MYUSERID_".length()));
                } catch (NumberFormatException e) { /* ignore */ }
            }
        }
        return -1L;
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

    private String extractMandat(Authentication auth) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if (a != null && a.startsWith("PROPERTY_MANDAT_")) {
                return a.substring("PROPERTY_MANDAT_".length()).toLowerCase();
            }
        }
        return "default";
    }
}
