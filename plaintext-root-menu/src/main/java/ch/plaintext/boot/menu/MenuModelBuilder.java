/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.menu;

import ch.plaintext.I18nProvider;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.MenuModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.function.Supplier;

/**
 * Builder for creating PrimeFaces MenuModel from annotated menu items
 */
@Slf4j
public class MenuModelBuilder {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private I18nProvider i18nProvider;

    /**
     * Creates a language supplier that reads the current user's language preference.
     * Uses ApplicationContext to lazily resolve the session-scoped UserPreferencesBackingBean.
     */
    private Supplier<String> createLanguageSupplier() {
        return () -> {
            try {
                Object bean = applicationContext.getBean("userPreferencesBackingBean");
                if (bean != null) {
                    java.lang.reflect.Method method = bean.getClass().getMethod("getLanguage");
                    return (String) method.invoke(bean);
                }
            } catch (Exception e) {
                log.debug("Could not resolve user language, using default 'de': {}", e.getMessage());
            }
            return "de";
        };
    }

    /**
     * Creates a PrimefacesMenuItem with i18n support injected.
     */
    private PrimefacesMenuItem createMenuItem(AbstractMenuItem item, Supplier<String> langSupplier) {
        PrimefacesMenuItem menuItem = new PrimefacesMenuItem(item);
        if (i18nProvider != null) {
            menuItem.setI18nProvider(i18nProvider);
            menuItem.setLanguageSupplier(langSupplier);
        }
        return menuItem;
    }

    /**
     * Creates a PrimefacesSubmenu with i18n support injected.
     */
    private PrimefacesSubmenu createSubmenu(AbstractMenuItem item, Supplier<String> langSupplier) {
        PrimefacesSubmenu submenu = new PrimefacesSubmenu(item);
        if (i18nProvider != null) {
            submenu.setI18nProvider(i18nProvider);
            submenu.setLanguageSupplier(langSupplier);
        }
        return submenu;
    }

    /**
     * Build a MenuModel from all registered MenuItemImpl beans
     * @return the constructed MenuModel
     */
    public MenuModel buildMenuModel() {
        DefaultMenuModel menuModel = new DefaultMenuModel();
        Supplier<String> langSupplier = createLanguageSupplier();

        // Get all MenuItemImpl beans from Spring context
        Map<String, MenuItemImpl> menuBeans = applicationContext.getBeansOfType(MenuItemImpl.class);
        List<MenuItemImpl> allMenuItems = new ArrayList<>(menuBeans.values());

        // Update dynamic badges before building menu
        updateDynamicBadges(allMenuItems);

        if (allMenuItems.isEmpty()) {
            log.warn("No menu items found in Spring context");
            return menuModel;
        }

        // Filter only visible items
        List<MenuItemImpl> visibleItems = allMenuItems.stream()
            .filter(MenuItemImpl::isOn)
            .toList();

        if (visibleItems.isEmpty()) {
            log.warn("No visible menu items found");
            return menuModel;
        }

        // Sort by order
        visibleItems = new ArrayList<>(visibleItems);
        visibleItems.sort(Comparator.comparingInt(MenuItemImpl::getOrder));

        // Build hierarchy using two-pass algorithm
        Map<String, PrimefacesSubmenu> submenuMap = new HashMap<>();
        Map<String, MenuItemImpl> itemMap = new HashMap<>();

        // First pass: Create all menu items and submenus, register them by title
        for (MenuItemImpl item : visibleItems) {
            itemMap.put(item.getTitle(), item);

            // If item has children, create a submenu for it
            if (hasChildren(item.getTitle(), visibleItems)) {
                PrimefacesSubmenu submenu = createSubmenu(item, langSupplier);
                submenuMap.put(item.getTitle(), submenu);
                log.debug("Created submenu: {}", item.getTitle());
            }
        }

        // Second pass: Build the hierarchy by adding items to their parents
        for (MenuItemImpl item : visibleItems) {
            String parent = item.getParent();

            if (parent == null || parent.trim().isEmpty()) {
                // Root level item
                if (hasChildren(item.getTitle(), visibleItems)) {
                    PrimefacesSubmenu submenu = submenuMap.get(item.getTitle());
                    menuModel.getElements().add(submenu);
                    log.debug("Added root submenu: {}", item.getTitle());
                } else {
                    PrimefacesMenuItem menuItem = createMenuItem(item, langSupplier);
                    menuModel.getElements().add(menuItem);
                    log.debug("Added root menu item: {}", item.getTitle());
                }
            } else {
                // Child item - find parent submenu
                PrimefacesSubmenu parentSubmenu = submenuMap.get(parent);

                if (parentSubmenu == null) {
                    log.warn("Parent submenu '{}' not found for item '{}' - skipping", parent, item.getTitle());
                    continue;
                }

                // Check if this item has children
                if (hasChildren(item.getTitle(), visibleItems)) {
                    PrimefacesSubmenu submenu = submenuMap.get(item.getTitle());
                    parentSubmenu.getElements().add(submenu);
                    log.debug("Added submenu '{}' to parent '{}'", item.getTitle(), parent);
                } else {
                    PrimefacesMenuItem menuItem = createMenuItem(item, langSupplier);
                    parentSubmenu.getElements().add(menuItem);
                    log.debug("Added menu item '{}' to parent '{}'", item.getTitle(), parent);
                }
            }
        }

        log.info("Built menu model with {} top-level elements", menuModel.getElements().size());
        return menuModel;
    }

    private boolean hasChildren(String title, List<MenuItemImpl> visibleItems) {
        return visibleItems.stream()
            .anyMatch(item -> title.equals(item.getParent()));
    }

    /**
     * Update dynamic badges for menu items that have badge providers
     */
    private void updateDynamicBadges(List<MenuItemImpl> menuItems) {
        // Try to get ChatMenuBadgeProvider if available
        try {
            Object badgeProvider = applicationContext.getBean("chatMenuBadgeProvider");
            if (badgeProvider != null) {
                // Find Chat menu item and update its badge
                for (MenuItemImpl item : menuItems) {
                    if ("Chat".equals(item.getTitle()) && (item.getParent() == null || item.getParent().isEmpty())) {
                        try {
                            // Use reflection to call getBadgeCount
                            java.lang.reflect.Method method = badgeProvider.getClass().getMethod("getBadgeCount");
                            String badgeCount = (String) method.invoke(badgeProvider);
                            item.setBadge(badgeCount);
                            log.debug("Updated Chat menu badge: {}", badgeCount);
                        } catch (Exception e) {
                            log.debug("Could not update Chat menu badge", e);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Badge provider not available, that's ok
            log.debug("ChatMenuBadgeProvider not available: {}", e.getMessage());
        }
    }
}
