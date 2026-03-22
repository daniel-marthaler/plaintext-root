/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.filelist.menu;

import ch.plaintext.boot.menu.MenuAnnotation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilelistSubmenuTest {

    @Test
    void classHasMenuAnnotation() {
        MenuAnnotation annotation = FilelistSubmenu.class.getAnnotation(MenuAnnotation.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.title()).isEqualTo("Dateiliste");
        assertThat(annotation.link()).isEqualTo("filelist.html");
        assertThat(annotation.parent()).isEqualTo("Admin");
        assertThat(annotation.order()).isEqualTo(7);
        assertThat(annotation.icon()).isEqualTo("pi pi-folder");
    }
}
