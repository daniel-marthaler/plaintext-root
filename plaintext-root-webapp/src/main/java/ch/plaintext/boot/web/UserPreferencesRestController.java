/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.web;

import ch.plaintext.boot.plugins.jsf.userprofile.UserPreferencesBackingBean;
import ch.plaintext.boot.plugins.jsf.userprofile.UserPreference;
import ch.plaintext.boot.plugins.jsf.userprofile.UserPrefsSimpleStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/preferences")
@Tag(name = "User Preferences", description = "Manage user UI preferences such as theme, menu mode, and dark mode")
public class UserPreferencesRestController {

    private static final Set<String> VALID_DARK_MODES = Set.of("dark", "light");
    private static final Set<String> VALID_MENU_MODES = Set.of("static", "overlay", "slim", "horizontal");
    private static final Set<String> VALID_INPUT_STYLES = Set.of("outlined", "filled");
    private static final int MAX_PARAM_LENGTH = 100;

    @Autowired
    private UserPrefsSimpleStorage storage;

    @Autowired
    private UserPreferencesBackingBean userPreferencesBackingBean;

    @Operation(summary = "Save user preferences",
               description = "Persists UI preferences (theme, dark mode, menu layout, etc.) for the currently authenticated user. "
                           + "Only provided parameters are updated; omitted parameters retain their previous values. "
                           + "The dark-mode setting is also stored as a cookie for consistent login-page theming.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferences saved successfully"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error while saving preferences")
    })
    @PostMapping("/save")
    public ResponseEntity<String> savePreferences(
            @Parameter(description = "PrimeFaces component theme name") @RequestParam(required = false) String componentTheme,
            @Parameter(description = "Dark mode setting (e.g. 'dark' or 'light')") @RequestParam(required = false) String darkMode,
            @Parameter(description = "Menu mode (e.g. 'static', 'overlay', 'slim')") @RequestParam(required = false) String menuMode,
            @Parameter(description = "Topbar theme identifier") @RequestParam(required = false) String topbarTheme,
            @Parameter(description = "Menu theme identifier") @RequestParam(required = false) String menuTheme,
            @Parameter(description = "Input style (e.g. 'outlined', 'filled')") @RequestParam(required = false) String inputStyle,
            @Parameter(description = "Whether the menu is pinned in static mode") @RequestParam(required = false) String menuStatic,
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

            // Validate input parameters
            validateParam("darkMode", darkMode, VALID_DARK_MODES);
            validateParam("menuMode", menuMode, VALID_MENU_MODES);
            validateParam("inputStyle", inputStyle, VALID_INPUT_STYLES);
            validateLength("componentTheme", componentTheme);
            validateLength("topbarTheme", topbarTheme);
            validateLength("menuTheme", menuTheme);

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

    private void validateParam(String name, String value, Set<String> allowed) {
        if (value != null && !value.isEmpty() && !allowed.contains(value)) {
            throw new IllegalArgumentException(
                    "Invalid value for '" + name + "': '" + value + "'. Allowed: " + allowed);
        }
    }

    private void validateLength(String name, String value) {
        if (value != null && value.length() > MAX_PARAM_LENGTH) {
            throw new IllegalArgumentException(
                    "Parameter '" + name + "' exceeds maximum length of " + MAX_PARAM_LENGTH);
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
