/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.web;

import ch.plaintext.boot.plugins.jsf.userprofile.UserPreferencesBackingBean;
import ch.plaintext.boot.plugins.jsf.userprofile.UserPreference;
import ch.plaintext.boot.plugins.jsf.userprofile.UserPrefsSimpleStorage;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/preferences")
public class UserPreferencesRestController {

    @Autowired
    private UserPrefsSimpleStorage storage;

    @Autowired
    private UserPreferencesBackingBean userPreferencesBackingBean;

    @PostMapping("/save")
    public ResponseEntity<String> savePreferences(
            @RequestParam(required = false) String componentTheme,
            @RequestParam(required = false) String darkMode,
            @RequestParam(required = false) String menuMode,
            @RequestParam(required = false) String topbarTheme,
            @RequestParam(required = false) String menuTheme,
            @RequestParam(required = false) String inputStyle,
            @RequestParam(required = false) String menuStatic,
            HttpServletResponse response) {
        try {
            log.debug("🔵 REST /api/preferences/save called with: componentTheme={}, darkMode={}, menuMode={}, topbarTheme={}, menuTheme={}, inputStyle={}, menuStatic={}",
                    componentTheme, darkMode, menuMode, topbarTheme, menuTheme, inputStyle, menuStatic);

            // Get current user from security context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body("NOT_AUTHENTICATED");
            }

            User user = (User) auth.getPrincipal();
            String username = user.getUsername();

            // Load or create user prefs
            UserPreference prefs = storage.findByUniqueId(username);
            if (prefs == null) {
                prefs = new UserPreference();
                prefs.setUniqueId(username);
            }

            // Update only provided values
            if (componentTheme != null && !componentTheme.isEmpty()) {
                prefs.setComponentTheme(componentTheme);
            }
            if (darkMode != null && !darkMode.isEmpty()) {
                prefs.setDarkMode(darkMode);
            }
            if (menuMode != null && !menuMode.isEmpty()) {
                prefs.setMenuMode(menuMode);
            }
            if (topbarTheme != null && !topbarTheme.isEmpty()) {
                prefs.setTopbarTheme(topbarTheme);
            }
            if (menuTheme != null && !menuTheme.isEmpty()) {
                prefs.setMenuTheme(menuTheme);
            }
            if (inputStyle != null && !inputStyle.isEmpty()) {
                prefs.setInputStyle(inputStyle);
            }
            if (menuStatic != null && !menuStatic.isEmpty()) {
                prefs.setMenuStatic(Boolean.parseBoolean(menuStatic));
            }

            // Save to database
            storage.save(prefs);

            log.debug("✅ Saved preferences to DB for user {}: componentTheme={}, darkMode={}, menuMode={}, topbarTheme={}, menuTheme={}, inputStyle={}",
                username, prefs.getComponentTheme(), prefs.getDarkMode(), prefs.getMenuMode(),
                prefs.getTopbarTheme(), prefs.getMenuTheme(), prefs.getInputStyle());

            // CRITICAL: Save theme to cookie for consistent login experience
            if (darkMode != null && !darkMode.isEmpty()) {
                saveThemeToCookie(response, darkMode);
                log.debug("🍪 Saved theme to cookie: {}", darkMode);
            }

            // CRITICAL FIX: Also update the session-scoped UserPreferencesBackingBean
            // Otherwise, the session bean will have stale values on next page load
            if (userPreferencesBackingBean != null) {
                userPreferencesBackingBean.updateFromRestApi(componentTheme, darkMode, menuMode,
                        topbarTheme, menuTheme, inputStyle, menuStatic);
            } else {
                log.debug("⚠️ UserPreferencesBackingBean session bean not available - values will update on next login");
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error saving preferences", e);
            return ResponseEntity.status(500).body("ERROR: " + e.getMessage());
        }
    }

    /**
     * Saves the theme to a cookie for consistent login page theming.
     */
    private void saveThemeToCookie(HttpServletResponse response, String theme) {
        try {
            Cookie cookie = new Cookie("plaintext-theme", theme);
            cookie.setPath("/");
            cookie.setMaxAge(365 * 24 * 60 * 60); // 1 year
            cookie.setHttpOnly(false); // Allow JavaScript access
            response.addCookie(cookie);
        } catch (Exception e) {
            log.error("Error saving theme to cookie: " + e.getMessage(), e);
        }
    }
}
