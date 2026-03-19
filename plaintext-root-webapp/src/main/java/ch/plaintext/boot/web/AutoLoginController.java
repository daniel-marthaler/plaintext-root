/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.web;

import ch.plaintext.boot.plugins.security.PlaintextLoginEvent;
import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import ch.plaintext.boot.plugins.security.persistence.MyUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
public class AutoLoginController {

    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final MyUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${mad.autologin:false}")
    private boolean autoLoginEnabled;

    public AutoLoginController(UserDetailsService userDetailsService,
                               SecurityContextRepository securityContextRepository,
                               MyUserRepository userRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/autologin")
    public String autoLogin(@RequestParam(required = false) String key,
                           HttpServletRequest request,
                           HttpServletResponse response) {

        log.info("AutoLogin requested with key: {}", key);

        // Check if autologin is enabled
        if (!autoLoginEnabled) {
            log.warn("AutoLogin is disabled in configuration");
            return "redirect:/login";
        }

        // Validate key is provided
        if (key == null || key.isEmpty()) {
            log.warn("No AutoLogin key provided");
            return "redirect:/login";
        }

        try {
            // Find user by autologinKey
            MyUserEntity user = userRepository.findByAutologinKey(key);

            if (user == null) {
                log.warn("No user found with AutoLogin key: {}", key);
                return "redirect:/login";
            }

            // Load UserDetails from database
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

            // Create an authenticated token using the 3-parameter constructor
            // This constructor automatically sets authenticated=true
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );

            // Create security context
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authToken);
            SecurityContextHolder.setContext(context);

            // Save to session
            securityContextRepository.saveContext(context, request, response);

            log.info("AutoLogin successful for user: {}", user.getUsername());

            // Publish login event for discovery module
            try {
                Long userId = extractUserId(userDetails);
                String mandat = extractMandat(userDetails);
                String baseUrl = extractBaseUrl(request);
                eventPublisher.publishEvent(new PlaintextLoginEvent(this, user.getUsername(), userId, user.getUsername(), mandat, baseUrl));
                log.debug("Published PlaintextLoginEvent for auto-login user: {} baseUrl: {}", user.getUsername(), baseUrl);
            } catch (Exception e) {
                log.warn("Failed to publish login event for auto-login: {}", e.getMessage());
            }

            // Check if user has a startpage configured
            String redirectUrl = "index.html";
            if (user.getStartpage() != null && !user.getStartpage().isEmpty()) {
                redirectUrl = user.getStartpage();
                log.info("Redirecting to user's startpage: {}", redirectUrl);
            }

            return "redirect:/" + redirectUrl;

        } catch (Exception e) {
            log.error("AutoLogin failed", e);
            return "redirect:/login";
        }
    }

    private Long extractUserId(UserDetails userDetails) {
        for (GrantedAuthority ga : userDetails.getAuthorities()) {
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

    private String extractMandat(UserDetails userDetails) {
        for (GrantedAuthority ga : userDetails.getAuthorities()) {
            String a = ga.getAuthority();
            if (a != null && a.startsWith("PROPERTY_MANDAT_")) {
                return a.substring("PROPERTY_MANDAT_".length()).toLowerCase();
            }
        }
        return "default";
    }
}
