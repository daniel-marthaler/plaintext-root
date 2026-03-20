/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.wertelisten.menu;

import ch.plaintext.boot.menu.MenuAnnotation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WertelistenSubmenuTest {

    @Test
    void classIsAnnotatedWithMenuAnnotation() {
        MenuAnnotation annotation = WertelistenSubmenu.class.getAnnotation(MenuAnnotation.class);
        assertThat(annotation).isNotNull();
    }

    @Test
    void menuAnnotationHasCorrectTitle() {
        MenuAnnotation annotation = WertelistenSubmenu.class.getAnnotation(MenuAnnotation.class);
        assertThat(annotation.title()).isEqualTo("Wertelisten");
    }

    @Test
    void menuAnnotationHasCorrectLink() {
        MenuAnnotation annotation = WertelistenSubmenu.class.getAnnotation(MenuAnnotation.class);
        assertThat(annotation.link()).isEqualTo("wertelisten.html");
    }

    @Test
    void menuAnnotationHasCorrectParent() {
        MenuAnnotation annotation = WertelistenSubmenu.class.getAnnotation(MenuAnnotation.class);
        assertThat(annotation.parent()).isEqualTo("Admin");
    }

    @Test
    void menuAnnotationHasCorrectOrder() {
        MenuAnnotation annotation = WertelistenSubmenu.class.getAnnotation(MenuAnnotation.class);
        assertThat(annotation.order()).isEqualTo(3);
    }

    @Test
    void menuAnnotationHasCorrectIcon() {
        MenuAnnotation annotation = WertelistenSubmenu.class.getAnnotation(MenuAnnotation.class);
        assertThat(annotation.icon()).isEqualTo("pi pi-list");
    }

    @Test
    void canBeInstantiated() {
        WertelistenSubmenu submenu = new WertelistenSubmenu();
        assertThat(submenu).isNotNull();
    }
}
