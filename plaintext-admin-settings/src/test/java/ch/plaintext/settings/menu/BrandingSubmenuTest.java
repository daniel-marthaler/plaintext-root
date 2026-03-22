/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.settings.menu;

import ch.plaintext.boot.menu.MenuAnnotation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrandingSubmenuTest {

    @Test
    void classHasMenuAnnotation() {
        MenuAnnotation annotation = BrandingSubmenu.class.getAnnotation(MenuAnnotation.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.title()).isEqualTo("Branding");
        assertThat(annotation.link()).isEqualTo("branding.html");
        assertThat(annotation.parent()).isEqualTo("Root");
        assertThat(annotation.order()).isEqualTo(2);
        assertThat(annotation.icon()).isEqualTo("pi pi-palette");
    }
}
