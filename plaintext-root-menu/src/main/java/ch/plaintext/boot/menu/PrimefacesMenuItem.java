package ch.plaintext.boot.menu;

import org.primefaces.model.menu.DefaultMenuItem;

/**
 * PrimeFaces DefaultMenuItem wrapper for AbstractMenuItem
 */
public class PrimefacesMenuItem extends DefaultMenuItem {

    private final AbstractMenuItem item;

    public PrimefacesMenuItem(AbstractMenuItem item) {
        this.item = item;
        this.setValue(item.getTitle());
        this.setUrl(item.getCommand());
        this.setIcon(item.getIc());
    }

    @Override
    public Object getValue() {
        return item.getTitle();
    }

    @Override
    public boolean isRendered() {
        return item.isOn();
    }
}
