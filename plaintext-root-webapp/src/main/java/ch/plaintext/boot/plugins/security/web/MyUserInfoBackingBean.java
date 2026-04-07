/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import ch.plaintext.boot.plugins.security.persistence.MyUserRepository;
import ch.plaintext.settings.ISetupConfigService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Data
@Scope("session")
public class MyUserInfoBackingBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Autowired
    private MyUserRepository userRepository;

    @Autowired
    private PlaintextSecurity plaintextSecurity;

    @Autowired
    private ISetupConfigService setupConfigService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Advanced mode flag (activated via Ctrl+Shift+D)
    private boolean advancedMode = false;

    // Password change fields
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    public String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User user) {
                return user.getUsername();
            }
            if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
                return oidcUser.getEmail() != null ? oidcUser.getEmail() : auth.getName();
            }
            return auth.getName();
        }
        return "N/A";
    }

    public List<String> getRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<String> getProperties() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("PROPERTY_"))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public String getMandat() {
        return getProperties().stream()
                .filter(p -> p.startsWith("PROPERTY_MANDAT_"))
                .map(p -> p.substring("PROPERTY_MANDAT_".length()))
                .findFirst()
                .orElse("N/A");
    }

    public String getStartpage() {
        return getProperties().stream()
                .filter(p -> p.startsWith("PROPERTY_STARTPAGE_"))
                .map(p -> p.substring("PROPERTY_STARTPAGE_".length()))
                .findFirst()
                .orElse("N/A");
    }

    /**
     * Returns the user's startpage with fallback to index.html.
     * If startpage is null, empty, or "N/A", returns "index.html".
     * Otherwise returns the configured startpage with faces-redirect.
     */
    public String getStartpageOrDefault() {
        String startpage = getStartpage();

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
    }

    public String getMyUserId() {
        return getProperties().stream()
                .filter(p -> p.startsWith("PROPERTY_MYUSERID_"))
                .map(p -> p.substring("PROPERTY_MYUSERID_".length()))
                .findFirst()
                .orElse("N/A");
    }

    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    public boolean isAccountNonExpired() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            return ((org.springframework.security.core.userdetails.User) auth.getPrincipal()).isAccountNonExpired();
        }
        return false;
    }

    public boolean isAccountNonLocked() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            return ((org.springframework.security.core.userdetails.User) auth.getPrincipal()).isAccountNonLocked();
        }
        return false;
    }

    public boolean isCredentialsNonExpired() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            return ((org.springframework.security.core.userdetails.User) auth.getPrincipal()).isCredentialsNonExpired();
        }
        return false;
    }

    public boolean isEnabled() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            return ((org.springframework.security.core.userdetails.User) auth.getPrincipal()).isEnabled();
        }
        return false;
    }

    /**
     * Returns the autologin key for the current user, or null if not available.
     */
    public String getAutologinKey() {
        if (setupConfigService == null || !setupConfigService.isAutologinEnabled(getMandat()) || userRepository == null) {
            return null;
        }

        String username = getUsername();
        if (username == null || "N/A".equals(username)) {
            return null;
        }

        MyUserEntity user = userRepository.findByUsername(username);
        if (user == null || user.getAutologinKey() == null || user.getAutologinKey().isEmpty()) {
            return null;
        }

        return user.getAutologinKey();
    }

    /**
     * Returns the complete autologin URL for the current user.
     */
    public String getAutologinUrl() {
        String key = getAutologinKey();
        if (key == null) {
            return null;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();

        String serverName = request.getServerName();
        String scheme = determineScheme(serverName, request.getScheme());
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        // Only add port if it's not the default port for the scheme
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath);
        url.append("/autologin?key=").append(key);

        return url.toString();
    }

    /**
     * Determines the scheme (http/https) based on the server name.
     * If the server name is not an IP address, https is used.
     * Otherwise, the request scheme is used.
     */
    private String determineScheme(String serverName, String requestScheme) {
        if (isIpAddress(serverName)) {
            return requestScheme;
        }
        return "https";
    }

    /**
     * Checks if the given string is an IP address (IPv4 or IPv6).
     */
    private boolean isIpAddress(String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            return false;
        }
        // Check for IPv4
        if (serverName.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            return true;
        }
        // Check for IPv6 (simple check for colon presence)
        return serverName.contains(":");
    }

    /**
     * Checks if autologin is enabled and the current user has an autologin key.
     */
    public boolean isAutologinKeyAvailable() {
        return getAutologinKey() != null;
    }

    /**
     * Generates a new autologin key for the current user and saves it to the database.
     */
    public void regenerateAutologinKey() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (setupConfigService == null || !setupConfigService.isAutologinEnabled(getMandat()) || userRepository == null) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Autologin ist nicht aktiviert."));
            return;
        }

        String username = getUsername();
        if (username == null || "N/A".equals(username)) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Benutzer konnte nicht ermittelt werden."));
            return;
        }

        MyUserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Benutzer nicht gefunden."));
            return;
        }

        // Generate new autologin key
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder token = new StringBuilder();
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 35; i++) {
            token.append(chars.charAt(random.nextInt(chars.length())));
        }

        user.setAutologinKey(token.toString());
        userRepository.save(user);

        context.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Auto-Login Link wurde erfolgreich erneuert."));
    }

    /**
     * Navigates to the user's configured startpage or falls back to index.html.
     * This method is used by JSF navigation from the access-denied page.
     */
    public String navigateToStartpage() {
        if (plaintextSecurity != null) {
            return plaintextSecurity.getStartpageOrDefault();
        }
        return "/index.html?faces-redirect=true";
    }

    /**
     * Toggles the advanced mode (activated via Ctrl+Shift+D).
     * Shows additional fields like Account Status, Weitere Eigenschaften, and Rolle zuweisen.
     */
    public void toggleAdvancedMode() {
        this.advancedMode = !this.advancedMode;

        // Pass the new state to JavaScript via callback parameter
        org.primefaces.PrimeFaces.current().ajax().addCallbackParam("advancedModeEnabled", this.advancedMode);
    }

    /**
     * Changes the password for the current user.
     */
    public void changePassword() {
        FacesContext context = FacesContext.getCurrentInstance();

        // Validate input
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            context.addMessage("passwordMessages",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Bitte geben Sie Ihr aktuelles Passwort ein."));
            return;
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            context.addMessage("passwordMessages",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Bitte geben Sie ein neues Passwort ein."));
            return;
        }

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            context.addMessage("passwordMessages",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Bitte bestätigen Sie das neue Passwort."));
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            context.addMessage("passwordMessages",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Die neuen Passwörter stimmen nicht überein."));
            return;
        }

        // Get current user
        String username = getUsername();
        if (username == null || "N/A".equals(username)) {
            context.addMessage("passwordMessages",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Benutzer konnte nicht ermittelt werden."));
            return;
        }

        MyUserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            context.addMessage("passwordMessages",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Benutzer nicht gefunden."));
            return;
        }

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            context.addMessage("passwordMessages",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Das aktuelle Passwort ist nicht korrekt."));
            return;
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Clear fields
        currentPassword = null;
        newPassword = null;
        confirmPassword = null;

        context.addMessage("passwordMessages",
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Passwort wurde erfolgreich geändert."));
    }
}
