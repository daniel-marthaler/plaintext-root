/*
 * Copyright (C) plaintext.ch, 2024.
 */
package ch.plaintext;

import java.util.List;

/**
 * Interface for accessing registered menu items in the application.
 * <p>
 * This interface provides methods to query all available menu items,
 * which can be used for configuration UIs or admin panels.
 * </p>
 *
 * @author plaintext.ch
 * @since 1.42.0
 */
public interface MenuRegistry {

    /**
     * Gets all registered menu titles.
     * Returns the full titles including parent hierarchy.
     * E.g., "Root | Mandate", "Zeiterfassung | Zeiterfassung Einstellungen"
     *
     * @return list of all menu titles
     */
    List<String> getAllMenuTitles();

    /**
     * Gets all registered menu items with their full details.
     *
     * @return list of all menu items
     */
    List<MenuItem> getAllMenuItems();

    /**
     * Represents a menu item with its properties.
     */
    interface MenuItem {
        String getTitle();
        String getParent();
        String getFullTitle();
        String getIcon();
        String getLink();
        int getOrder();
        List<String> getRoles();

        /**
         * Checks if this menu item should be visible/accessible to the current user.
         * This combines both role-based and mandate-based visibility checks.
         *
         * @return true if the menu item is visible, false otherwise
         */
        boolean isOn();
    }
}
