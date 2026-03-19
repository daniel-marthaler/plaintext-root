/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import ch.plaintext.boot.plugins.security.persistence.MyUserRepository;
import ch.plaintext.menuesteuerung.model.MandateMenuConfig;
import ch.plaintext.menuesteuerung.persistence.MandateMenuConfigRepository;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides static access to security-related functionality.
 * This class allows static method calls to retrieve current user information
 * from the Spring Security context.
 */
@Component
@Named("plaintextSecurity")
@Slf4j
public class PlaintextSecurityImpl implements PlaintextSecurity {

    private static final String SESSION_ORIGINAL_USER_ID = "impersonation.originalUserId";
    private static final String SESSION_ORIGINAL_AUTH = "impersonation.originalAuth";
    private static final String SYSTEM_USER = "SYSTEM";

    private static PlaintextSecurityImpl instance;

    private final MyUserRepository userRepository;
    private final MandateMenuConfigRepository mandateMenuConfigRepository;

    public PlaintextSecurityImpl(MyUserRepository userRepository,
                                  MandateMenuConfigRepository mandateMenuConfigRepository) {
        this.userRepository = userRepository;
        this.mandateMenuConfigRepository = mandateMenuConfigRepository;
    }

    @PostConstruct
    private void init() {
        PlaintextSecurityImpl.instance = this;
    }

    /**
     * Gets the mandat for the currently authenticated user.
     *
     * @return The mandat string from the user's roles, or "default" if no mandat role is found
     * Returns "NO_AUTH" if no authentication is present
     * Returns "NO_USER" if the user cannot be found in the database
     */
    @Override
    public String getMandat() {
        if (instance == null) {
            log.warn("PlaintextSecurityImpl instance not initialized");
            return "NO_INSTANCE";
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                return "NO_AUTH";
            }
            for(GrantedAuthority role: auth.getAuthorities()){
                if(role.toString().toLowerCase().contains("mandat")){
                    String result = role.toString().toLowerCase().split("_")[role.toString().split("_").length - 1];
                    return result;
                }
            }
            return "default";
        } catch (Exception e) {
            log.error("Error getting mandat", e);
            return "ERROR";
        }
    }

    @Override
    public Set<String> getAllMandate() {
            Set<String> mandanten = new HashSet<>();

            try {
                // 1. Mandate aus Benutzern laden
                List<MyUserEntity> allUsers = userRepository.findAll();
                for (MyUserEntity user : allUsers) {
                    String mandat = user.getMandat();
                    if (mandat != null && !mandat.trim().isEmpty()) {
                        mandanten.add(mandat.toLowerCase());
                    }
                }
                log.debug("Found {} unique mandanten from users: {}", mandanten.size(), mandanten);

                // 2. Mandate aus MandateMenuConfig laden
                List<MandateMenuConfig> menuConfigs = mandateMenuConfigRepository.findAll();
                for (MandateMenuConfig config : menuConfigs) {
                    String mandat = config.getMandateName();
                    if (mandat != null && !mandat.trim().isEmpty()) {
                        mandanten.add(mandat.toLowerCase());
                    }
                }
                log.debug("Found {} total unique mandanten after adding menu configs: {}", mandanten.size(), mandanten);

            } catch (Exception e) {
                log.error("Error loading mandanten from database", e);
                // Fallback auf default
                mandanten.add("default");
            }

            // Wenn keine Mandate gefunden wurden, default hinzufügen
            if (mandanten.isEmpty()) {
                log.warn("No mandanten found in database, using 'default'");
                mandanten.add("default");
            }

            return mandanten;
    }

    /**
     * Sets the mandat for the currently authenticated user by injecting
     * a role of the form "PROPERTY_MANDAT_<value>" into the SecurityContext
     * and persisting the mandat in the database for the current user.
     *
     * @param mandat The mandat to set (e.g. "dev", "admin", "test")
     */
    public void setMandat(String mandat) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                log.warn("No authentication present – cannot set mandat");
                return;
            }

            // 1) Rollen im SecurityContext anpassen
            List<GrantedAuthority> newAuthorities = new ArrayList<>(auth.getAuthorities());

            // Alte Mandat-Rollen entfernen
            newAuthorities.removeIf(a -> a.getAuthority().toLowerCase().contains("mandat"));

            // Neue Mandat-Rolle hinzufügen
            String rolle = "PROPERTY_MANDAT_" + mandat.toLowerCase();
            newAuthorities.add(new SimpleGrantedAuthority(rolle));

            // Neue Authentication erzeugen (gleicher Principal, gleiche Credentials)
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    auth.getPrincipal(),
                    auth.getCredentials(),
                    newAuthorities
            );

            // In den SecurityContext zurückschreiben
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            log.info("Mandat (SecurityContext) updated to {}", rolle);

            // 2) Mandat in der Datenbank speichern
            Long userId = getId(); // nutzt bereits die Rollen, um myuserid rauszuziehen
            if (userId == null || userId <= 0) {
                log.warn("Cannot persist mandat – invalid userId: {}", userId);
                return;
            }

            userRepository.findById(userId).ifPresentOrElse(user -> {
                user.setMandat(mandat);
                userRepository.save(user);
                log.info("Mandat (DB) for user {} updated to {}", userId, mandat);
            }, () -> {
                log.warn("User with ID {} not found – cannot persist mandat", userId);
            });

        } catch (Exception e) {
            log.error("Error while setting mandat", e);
        }
    }


    @Override
    public Long getId() {
        if (instance == null) {
            log.warn("PlaintextSecurityImpl instance not initialized");
            return -1L;
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            for(GrantedAuthority role: auth.getAuthorities()){
                if(role.toString().toLowerCase().contains("myuserid")){
                    String digits = role.toString().toLowerCase().replaceAll("[^0-9]", "");
                    return digits.isEmpty() ? -1L : Long.parseLong(digits);
                }
            }
            return -1L;
        } catch (Exception e) {
            log.error("Error getting mandat", e);
            return -1L;
        }
    }

    @Override
    public String getUser() {
        if (instance == null) {
            log.warn("PlaintextSecurityImpl instance not initialized");
            return SYSTEM_USER;
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                log.debug("No authenticated user found, using SYSTEM");
                return SYSTEM_USER;
            }
            return auth.getName();
        } catch (Exception e) {
            log.error("Error getting user", e);
            return SYSTEM_USER;
        }
    }

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public String getMandatForUser(long userId) {
        if (instance == null) {
            log.warn("PlaintextSecurityImpl instance not initialized");
            return null;
        }
        try {
            MyUserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User with ID {} not found", userId);
                return null;
            }
            return user.getMandat();
        } catch (Exception e) {
            log.error("Error getting mandat for user {}", userId, e);
            return null;
        }
    }

    @Override
    public boolean ifGranted(String role) {
        if (role == null) return false;
        String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getAuthentication().getAuthorities().stream()
                .anyMatch(a -> normalized.equalsIgnoreCase(a.getAuthority()));
    }

    public List<String> getUsersForMandat(String mandat) {
        List<String> users = new ArrayList<>();

        if (mandat == null || mandat.trim().isEmpty()) {
            log.warn("Cannot get users for null or empty mandat");
            return users;
        }

        try {
            List<MyUserEntity> allUsers = userRepository.findAll();
            for (MyUserEntity user : allUsers) {
                String userMandat = user.getMandat();
                if (userMandat != null && userMandat.equalsIgnoreCase(mandat)) {
                    users.add(user.getUsername());
                    log.debug("Found user {} for mandat {}", user.getUsername(), mandat);
                }
            }
            log.info("Found {} users for mandat {}", users.size(), mandat);
        } catch (Exception e) {
            log.error("Error getting users for mandat {}", mandat, e);
        }

        return users;
    }

    @Override
    public String getStartpageOrDefault() {
        if (instance == null) {
            log.warn("PlaintextSecurityImpl instance not initialized");
            return "/index.html?faces-redirect=true";
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getAuthorities() == null) {
                return "/index.html?faces-redirect=true";
            }

            // Get startpage from properties
            String startpage = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("PROPERTY_STARTPAGE_"))
                    .map(a -> a.substring("PROPERTY_STARTPAGE_".length()))
                    .findFirst()
                    .orElse("N/A");

            // Check if startpage is null, empty, or "N/A"
            if (startpage == null || startpage.trim().isEmpty() || "N/A".equalsIgnoreCase(startpage)) {
                return "/index.html?faces-redirect=true";
            }

            // Ensure .xhtml or .html extension
            if (!startpage.endsWith(".xhtml") && !startpage.endsWith(".html")) {
                startpage = startpage + ".xhtml";
            }

            // Add leading slash if not present
            if (!startpage.startsWith("/")) {
                startpage = "/" + startpage;
            }

            return startpage + "?faces-redirect=true";
        } catch (Exception e) {
            log.error("Error getting startpage, returning default", e);
            return "/index.html?faces-redirect=true";
        }
    }

    @Override
    public boolean isImpersonating() {
        try {
            HttpSession session = getCurrentSession();
            if (session == null) {
                return false;
            }
            return session.getAttribute(SESSION_ORIGINAL_USER_ID) != null;
        } catch (Exception e) {
            log.error("Error checking impersonation status", e);
            return false;
        }
    }

    @Override
    public void startImpersonation(Long userId) {
        if (userId == null) {
            log.warn("Cannot start impersonation with null userId");
            return;
        }

        try {
            // Get current authentication and user ID
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            Long currentUserId = getId();

            if (currentUserId == null || currentUserId <= 0) {
                log.warn("Cannot start impersonation - invalid current user ID");
                return;
            }

            // Get the user to impersonate
            MyUserEntity targetUser = userRepository.findById(userId).orElse(null);
            if (targetUser == null) {
                log.warn("Cannot start impersonation - user {} not found", userId);
                return;
            }

            // Get session and clear all attributes except security-related ones
            HttpSession session = getCurrentSession();
            if (session == null) {
                log.warn("Cannot start impersonation - no session available");
                return;
            }

            // Store original authentication in session
            session.setAttribute(SESSION_ORIGINAL_USER_ID, currentUserId);
            session.setAttribute(SESSION_ORIGINAL_AUTH, currentAuth);

            // Build new authorities for target user
            List<GrantedAuthority> newAuthorities = new ArrayList<>();

            // Add user ID as authority
            newAuthorities.add(new SimpleGrantedAuthority("PROPERTY_MYUSERID_" + targetUser.getId()));

            // Add roles (with same logic as MyUserDetailsService)
            if (targetUser.getRoles() != null) {
                for (String role : targetUser.getRoles()) {
                    if (role.toLowerCase().contains("mandat")) {
                        newAuthorities.add(new SimpleGrantedAuthority(role.toUpperCase()));
                    } else {
                        newAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                    }
                }
            }

            // Add mandat
            if (targetUser.getMandat() != null && !targetUser.getMandat().isEmpty()) {
                newAuthorities.add(new SimpleGrantedAuthority("PROPERTY_MANDAT_" + targetUser.getMandat().toLowerCase()));
            }

            // Add startpage if available
            if (targetUser.getStartpage() != null && !targetUser.getStartpage().isEmpty()) {
                newAuthorities.add(new SimpleGrantedAuthority("PROPERTY_STARTPAGE_" + targetUser.getStartpage()));
            }

            // Create new authentication for target user
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    targetUser.getUsername(),
                    currentAuth.getCredentials(),
                    newAuthorities
            );

            // Update security context
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            log.info("Started impersonation: original user {} is now impersonating user {} ({})",
                    currentUserId, userId, targetUser.getUsername());

        } catch (Exception e) {
            log.error("Error starting impersonation for user {}", userId, e);
        }
    }

    @Override
    public void stopImpersonation() {
        try {
            HttpSession session = getCurrentSession();
            if (session == null) {
                log.warn("Cannot stop impersonation - no session available");
                return;
            }

            Authentication originalAuth = (Authentication) session.getAttribute(SESSION_ORIGINAL_AUTH);
            Long originalUserId = (Long) session.getAttribute(SESSION_ORIGINAL_USER_ID);

            if (originalAuth == null) {
                log.warn("Cannot stop impersonation - no original authentication stored");
                return;
            }

            // Restore original authentication
            SecurityContextHolder.getContext().setAuthentication(originalAuth);

            // Clear impersonation session attributes
            session.removeAttribute(SESSION_ORIGINAL_USER_ID);
            session.removeAttribute(SESSION_ORIGINAL_AUTH);

            log.info("Stopped impersonation - restored original user {}", originalUserId);

        } catch (Exception e) {
            log.error("Error stopping impersonation", e);
        }
    }

    @Override
    public Long getOriginalUserId() {
        try {
            HttpSession session = getCurrentSession();
            if (session == null) {
                return null;
            }
            return (Long) session.getAttribute(SESSION_ORIGINAL_USER_ID);
        } catch (Exception e) {
            log.error("Error getting original user ID", e);
            return null;
        }
    }

    /**
     * Logout the current user and redirect to login page
     */
    public String logout() {
        try {
            log.info("Logging out user: {}", getUser());

            // Invalidate session
            HttpSession session = getCurrentSession();
            if (session != null) {
                session.invalidate();
            }

            // Clear security context
            SecurityContextHolder.clearContext();

            return "/login.html?faces-redirect=true";
        } catch (Exception e) {
            log.error("Error during logout", e);
            return "/login.html?faces-redirect=true";
        }
    }

    /**
     * Helper method to get current HTTP session
     */
    private HttpSession getCurrentSession() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest().getSession(false);
        } catch (Exception e) {
            log.debug("Could not get current session", e);
            return null;
        }
    }
}
