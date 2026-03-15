package ch.plaintext.boot.menu;

import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.MenuModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * Builder for creating PrimeFaces MenuModel from annotated menu items
 */
@Slf4j
public class MenuModelBuilder {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Build a MenuModel from all registered MenuItemImpl beans
     * @return the constructed MenuModel
     */
    public MenuModel buildMenuModel() {
        DefaultMenuModel menuModel = new DefaultMenuModel();

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
                PrimefacesSubmenu submenu = new PrimefacesSubmenu(item);
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
                    PrimefacesMenuItem menuItem = new PrimefacesMenuItem(item);
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
                    PrimefacesMenuItem menuItem = new PrimefacesMenuItem(item);
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
