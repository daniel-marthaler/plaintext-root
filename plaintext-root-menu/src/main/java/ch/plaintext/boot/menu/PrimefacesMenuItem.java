/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
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
