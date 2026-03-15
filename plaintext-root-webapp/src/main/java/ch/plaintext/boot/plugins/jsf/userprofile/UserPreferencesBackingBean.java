/*
   Copyright 2009-2022 PrimeTek.

   Licensed under PrimeFaces Commercial License, Version 1.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   Licensed under PrimeFaces Commercial License, Version 1.0 (the "License");

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package ch.plaintext.boot.plugins.jsf.userprofile;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Session-scoped bean for managing user preferences with PrimeFaces integration.
 * Refactored to use composition instead of duplicating fields from UserPreference.
 *
 * REFACTORED: Removed field duplication - now delegates to UserPreference instance.
 * Single source of truth: all preference data stored in 'prefs' field.
 */
@Slf4j
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component("userPreferencesBackingBean")
public class UserPreferencesBackingBean implements Serializable {

    private static final String LAYOUT_MENU = "layout-menu";
    private static final String LAYOUT_TOPBAR = "layout-topbar";
    private static final String LAYOUT_HORIZONTAL = "layout-horizontal";

    @Autowired
    private transient UserPrefsSimpleStorage storage;

    /**
     * Single source of truth for all user preference data.
     * Replaces previous field duplication.
     * Marked transient as UserPreference is not Serializable.
     */
    private transient UserPreference prefs;

    @Getter
    @Setter
    private List<ComponentTheme> componentThemes = new ArrayList<>();

    @PostConstruct
    public void init() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        prefs = storage.findByUniqueId(user.getUsername());

        // Load theme from cookie if available (for seamless login experience)
        String cookieTheme = loadThemeFromCookie();

        if (prefs != null) {
            log.debug("🟢 Loaded preferences from DB for user {}: menuMode={}, darkMode={}, componentTheme={}, topbarTheme={}, menuTheme={}, inputStyle={}, menuStatic={}",
                    user.getUsername(), prefs.getMenuMode(), prefs.getDarkMode(), prefs.getComponentTheme(),
                    prefs.getTopbarTheme(), prefs.getMenuTheme(), prefs.getInputStyle(), prefs.isMenuStatic());

            // If cookie theme differs from DB, update DB to match cookie
            if (cookieTheme != null && !cookieTheme.equals(prefs.getDarkMode())) {
                log.debug("Cookie theme '{}' differs from DB '{}', updating DB to match cookie", cookieTheme, prefs.getDarkMode());
                prefs.setDarkMode(cookieTheme);
                prefs.setTopbarTheme(cookieTheme);
                prefs.setMenuTheme(cookieTheme);
                prefs.setLightLogo(!cookieTheme.equals("light"));
            }

            // CRITICAL FIX: Ensure themes are consistent with darkMode
            // Topbar and menu themes must match darkMode to render correctly
            boolean needsSync = false;
            if (!prefs.getTopbarTheme().equals(prefs.getDarkMode())) {
                log.debug("Syncing topbarTheme from '{}' to '{}' to match darkMode", prefs.getTopbarTheme(), prefs.getDarkMode());
                prefs.setTopbarTheme(prefs.getDarkMode());
                needsSync = true;
            }
            if (!prefs.getMenuTheme().equals(prefs.getDarkMode())) {
                log.debug("Syncing menuTheme from '{}' to '{}' to match darkMode", prefs.getMenuTheme(), prefs.getDarkMode());
                prefs.setMenuTheme(prefs.getDarkMode());
                needsSync = true;
            }
            // Migration: Set menuStatic to true if it was false (old default)
            if (!prefs.isMenuStatic()) {
                log.debug("Migrating menuStatic from false to true (new default: sidebar expanded)");
                prefs.setMenuStatic(true);
                needsSync = true;
            }
            if (needsSync) {
                save(); // Persist the corrected values
            }
        } else {
            prefs = new UserPreference();
            prefs.setUniqueId(user.getUsername());
            save();
        }

        componentThemes.add(new ComponentTheme("Blue", "blue", "#2c84d8"));
        componentThemes.add(new ComponentTheme("Green", "green", "#34B56F"));
        componentThemes.add(new ComponentTheme("Orange", "orange", "#FF810E"));
        componentThemes.add(new ComponentTheme("Turquoise", "turquoise", "#58AED3"));
        componentThemes.add(new ComponentTheme("Avocado", "avocado", "#AEC523"));
        componentThemes.add(new ComponentTheme("Purple", "purple", "#464DF2"));
        componentThemes.add(new ComponentTheme("Red", "red", "#FF9B7B"));
        componentThemes.add(new ComponentTheme("Yellow", "yellow", "#FFB340"));
    }

    public synchronized void save() {
        try {
            storage.save(prefs);
        } catch (Exception e) {
            log.error("Error saving user preferences: " + e.getMessage(), e);
        }
    }

    /**
     * Update fields directly from REST API without triggering PrimeFaces scripts.
     * This is used when the REST API saves preferences - we need to update the
     * session-scoped bean but can't call the normal setters (which use PrimeFaces).
     */
    public void updateFromRestApi(String componentTheme, String darkMode, String menuMode,
                                   String topbarTheme, String menuTheme, String inputStyle, String menuStatic) {
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
        log.debug("✅ Session bean updated via REST API");
    }

    // ==================== Delegating Getters ====================

    public String getDarkMode() {
        return prefs.getDarkMode();
    }

    public String getDarkMode2() {
        return prefs.getDarkMode();
    }

    public boolean isLightLogo() {
        return prefs.isLightLogo();
    }

    public String getComponentTheme() {
        return prefs.getComponentTheme();
    }

    public String getMenuTheme() {
        return prefs.getMenuTheme();
    }

    public String getTopbarTheme() {
        return prefs.getTopbarTheme();
    }

    public String getMenuMode() {
        return prefs.getMenuMode();
    }

    public String getInputStyle() {
        return prefs.getInputStyle();
    }

    public boolean isMenuStatic() {
        return prefs.isMenuStatic();
    }

    // ==================== Computed Properties ====================

    public String getLayout() {
        return "layout-" + prefs.getDarkMode();
    }

    public String getTheme() {
        return prefs.getComponentTheme() + '-' + prefs.getDarkMode();
    }

    public String getInputStyleClass() {
        return prefs.getInputStyle().equals("filled") ? "ui-input-filled" : "";
    }

    public String getMenuStaticClass() {
        // Only apply layout-static for sidebar mode
        // For horizontal/slim, layout-static doesn't make sense and causes flickering
        if ("layout-sidebar".equals(prefs.getMenuMode()) && prefs.isMenuStatic()) {
            return "layout-static";
        }
        return "";
    }

    // ==================== Setters with PrimeFaces Integration ====================

    public void setDarkMode2(String darkMode) {
        prefs.setDarkMode(darkMode);
        prefs.setMenuTheme(darkMode);
        prefs.setTopbarTheme(darkMode);
        prefs.setLightLogo(!darkMode.equals("light"));
        // Update data-theme attribute on HTML element to prevent flash
        PrimeFaces.current().executeScript("document.documentElement.setAttribute('data-theme', '" + darkMode + "')");
        // Save theme to cookie for consistent login experience
        saveThemeToCookie(darkMode);
        save();
    }

    public void toggleDarkMode() {
        String newTheme = "light".equals(prefs.getDarkMode()) ? "dark" : "light";
        setDarkMode2(newTheme);
    }

    /**
     * Loads the theme from cookie if available.
     * @return theme value from cookie or null if not found
     */
    private String loadThemeFromCookie() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context == null) {
                return null;
            }

            ExternalContext externalContext = context.getExternalContext();
            HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();

            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("plaintext-theme".equals(cookie.getName())) {
                        String theme = cookie.getValue();
                        if ("light".equals(theme) || "dark".equals(theme)) {
                            log.debug("Loaded theme from cookie: {}", theme);
                            return theme;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error loading theme from cookie: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Saves the current theme to a cookie.
     */
    private void saveThemeToCookie(String theme) {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context == null) {
                return;
            }

            ExternalContext externalContext = context.getExternalContext();
            HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

            Cookie cookie = new Cookie("plaintext-theme", theme);
            cookie.setPath("/");
            cookie.setMaxAge(365 * 24 * 60 * 60); // 1 year
            cookie.setHttpOnly(false); // Allow JavaScript access
            response.addCookie(cookie);
            log.debug("Saved theme to cookie: {}", theme);
        } catch (Exception e) {
            log.error("Error saving theme to cookie: " + e.getMessage(), e);
        }
    }

    public void setComponentTheme(String componentTheme) {
        prefs.setComponentTheme(componentTheme);
        save();
    }

    public void setMenuTheme(String menuTheme) {
        prefs.setMenuTheme(menuTheme);
        PrimeFaces.current().executeScript("PrimeFaces.FreyaConfigurator.changeSectionTheme('" + menuTheme + "' , '" + LAYOUT_MENU + "')");
        save();
    }

    public void setTopbarTheme(String topbarTheme) {
        prefs.setTopbarTheme(topbarTheme);
        prefs.setLightLogo(!topbarTheme.equals("light"));

        PrimeFaces.current().executeScript("PrimeFaces.FreyaConfigurator.changeSectionTheme('" + topbarTheme + "' , '" + LAYOUT_TOPBAR + "')");
        if (LAYOUT_HORIZONTAL.equals(prefs.getMenuMode())) {
            PrimeFaces.current().executeScript("PrimeFaces.FreyaConfigurator.changeSectionTheme('" + topbarTheme + "' , '" + LAYOUT_MENU + "')");
        }
        save();
    }

    public void setMenuMode(String menuMode) {
        prefs.setMenuMode(menuMode);
        if (LAYOUT_HORIZONTAL.equals(menuMode)) {
            prefs.setMenuTheme(prefs.getTopbarTheme());
            PrimeFaces.current().executeScript("PrimeFaces.FreyaConfigurator.changeSectionTheme('" + prefs.getMenuTheme() + "' , '" + LAYOUT_MENU + "')");
        }
        PrimeFaces.current().executeScript("PrimeFaces.FreyaConfigurator.changeMenuMode('" + menuMode + "')");
        save();
    }

    public void setInputStyle(String inputStyle) {
        prefs.setInputStyle(inputStyle);
        PrimeFaces.current().executeScript("PrimeFaces.FreyaConfigurator.updateInputStyle('" + inputStyle + "')");
        save();
    }

    public void setMenuStatic(boolean menuStatic) {
        prefs.setMenuStatic(menuStatic);
        save();
    }

    public void toggleMenuStatic() {
        prefs.setMenuStatic(!prefs.isMenuStatic());
        save();
    }

    public void onMenuTypeChange() {
        if (LAYOUT_HORIZONTAL.equals(prefs.getMenuMode())) {
            prefs.setMenuTheme(prefs.getTopbarTheme());
            PrimeFaces.current().executeScript("PrimeFaces.FreyaConfigurator.changeSectionTheme('" + prefs.getMenuTheme() + "' , '" + LAYOUT_MENU + "')");
        }
        save();
    }

    // ==================== Inner Class ====================

    public static class ComponentTheme implements Serializable {
        String name;
        String file;
        String color;

        public ComponentTheme(String name, String file, String color) {
            this.name = name;
            this.file = file;
            this.color = color;
        }

        public String getName() {
            return this.name;
        }

        public String getFile() {
            return this.file;
        }

        public String getColor() {
            return this.color;
        }
    }

}
