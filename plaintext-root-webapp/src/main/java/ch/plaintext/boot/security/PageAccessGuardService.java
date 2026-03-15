/*
 * Copyright (C) plaintext.ch, 2024.
 */
package ch.plaintext.boot.security;

import ch.plaintext.MenuRegistry;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Service für Page Access Control basierend auf Menu-Sichtbarkeit.
 * Prüft ob ein Benutzer Zugriff auf eine JSF View hat, indem die
 * MenuItem.isOn() Methode verwendet wird. Diese prüft sowohl:
 * - Rollen-basierte Sichtbarkeit (via SecurityProvider)
 * - Mandate-basierte Sichtbarkeit (via MenuVisibilityProvider)
 *
 * @author plaintext.ch
 * @since 1.42.0
 */
@Service
@Slf4j
public class PageAccessGuardService {

    private final MenuRegistry menuRegistry;

    /**
     * Systemseiten die immer erreichbar sein sollen, unabhängig von Menü-Konfiguration
     */
    private static final Set<String> SYSTEM_PAGES = Set.of(
            "/home.xhtml",
            "/index.xhtml",
            "/access-denied.xhtml",
            "/error.xhtml",
            "/login.xhtml"
    );

    @Autowired
    public PageAccessGuardService(MenuRegistry menuRegistry) {
        this.menuRegistry = menuRegistry;
    }

    /**
     * Prüft ob der aktuelle Benutzer Zugriff auf die angegebene View hat.
     * Berücksichtigt BEIDE:
     * - Rollen-basierte Sichtbarkeit (MenuItem.roles via SecurityProvider)
     * - Mandate-basierte Sichtbarkeit (via MenuVisibilityProvider)
     *
     * @param viewId JSF View ID (z.B. "/kontakte.xhtml")
     * @return true wenn Zugriff erlaubt, false sonst
     */
    public boolean hasAccessToView(String viewId) {
        if (viewId == null || viewId.isEmpty()) {
            log.trace("View ID is null or empty, allowing access");
            return true;
        }

        // Systemseiten sind immer erlaubt
        if (SYSTEM_PAGES.contains(viewId)) {
            log.trace("System page '{}' - allowing access", viewId);
            return true;
        }

        // Konvertiere View ID zu URL (z.B. /kontakte.xhtml -> kontakte.html)
        // Entferne führenden Slash, da Menu-Commands auch keinen haben
        String pageUrl = viewId.replace(".xhtml", ".html");
        if (pageUrl.startsWith("/")) {
            pageUrl = pageUrl.substring(1);
        }

        try {
            // Suche MenuItemImpl das auf diese Seite zeigt
            // Verwende MenuItemImpl direkt um Classloader-Probleme zu vermeiden
            List<ch.plaintext.boot.menu.MenuItemImpl> allMenuItems =
                    ((ch.plaintext.boot.menu.MenuRegistryImpl) menuRegistry).getAllMenuItemsImpl();

            log.debug("PageAccessGuard: Checking {} menu items for view '{}' (looking for command='{}')",
                    allMenuItems.size(), viewId, pageUrl);

            for (ch.plaintext.boot.menu.MenuItemImpl item : allMenuItems) {
                String menuLink = item.getCommand();
                log.trace("PageAccessGuard: Menu '{}' has command='{}'", item.buildFullTitle(), menuLink);

                // Prüfe ob Menüpunkt auf diese Seite zeigt (case-insensitive)
                if (menuLink != null && menuLink.toLowerCase().equals(pageUrl.toLowerCase())) {
                    String fullTitle = item.buildFullTitle();

                    // WICHTIG: isOn() prüft BEIDES:
                    // 1. Rollen (über SecurityProvider.hasRole())
                    // 2. Mandate (über MenuVisibilityProvider.isMenuVisible())
                    boolean isVisible = item.isOn();

                    if (!isVisible) {
                        log.warn("SECURITY: Access denied to view '{}' - menu '{}' is not visible (role or mandate restriction)",
                                viewId, fullTitle);
                        return false;
                    }

                    // Menü ist sichtbar (Rolle UND Mandate OK) = Zugriff erlaubt
                    log.debug("Access granted to view '{}' via visible menu '{}'", viewId, fullTitle);
                    return true;
                }
            }

            // Keine Menüzuordnung gefunden
            // Erlauben, damit Seiten ohne Menü (z.B. Hilfe-Seiten) auch funktionieren
            log.debug("No menu found for view '{}' - allowing access (no menu restriction)", viewId);
            return true;

        } catch (Exception e) {
            log.error("Error checking access to view '{}': {}", viewId, e.getMessage(), e);
            // Im Fehlerfall erlauben, um Systemausfall zu vermeiden
            return true;
        }
    }

    /**
     * Redirect zu Access Denied Seite
     */
    public void redirectToAccessDenied() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            log.error("FacesContext is null, cannot redirect to access denied");
            return;
        }

        ExternalContext externalContext = context.getExternalContext();
        String contextPath = externalContext.getRequestContextPath();
        String redirectUrl = contextPath + "/access-denied.html";

        log.debug("Redirecting to access denied page: {}", redirectUrl);
        externalContext.redirect(redirectUrl);
        context.responseComplete();
    }
}
