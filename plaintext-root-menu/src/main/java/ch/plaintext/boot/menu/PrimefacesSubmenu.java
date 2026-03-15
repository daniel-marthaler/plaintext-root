package ch.plaintext.boot.menu;

import org.primefaces.model.menu.DefaultSubMenu;

/**
 * PrimeFaces DefaultSubMenu wrapper for AbstractMenuItem
 */
public class PrimefacesSubmenu extends DefaultSubMenu {

    private final AbstractMenuItem item;

    public PrimefacesSubmenu(AbstractMenuItem item) {
        this.item = item;
        this.setLabel(item.getTitle());
        this.setIcon(item.getIc());
    }

    @Override
    public String getLabel() {
        return item.getTitle();
    }

    @Override
    public boolean isRendered() {
        return item.isOn();
    }

    /**
     * Get the menu item implementation
     */
    public MenuItemImpl getMenuItem() {
        if (item instanceof MenuItemImpl) {
            return (MenuItemImpl) item;
        }
        return null;
    }
}
