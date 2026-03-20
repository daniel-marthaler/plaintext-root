/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.menu;

import ch.plaintext.boot.menu.MenuAnnotation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryStatsMenuTest {

    @Test
    void classHasMenuAnnotation() {
        MenuAnnotation annotation = DiscoveryStatsMenu.class.getAnnotation(MenuAnnotation.class);

        assertNotNull(annotation);
        assertEquals("Discovery Stats", annotation.title());
        assertEquals("discoveryStats.html", annotation.link());
        assertEquals("Admin", annotation.parent());
        assertEquals(50, annotation.order());
        assertEquals("pi pi-chart-line", annotation.icon());
        assertArrayEquals(new String[]{"ROOT"}, annotation.roles());
    }

    @Test
    void canBeInstantiated() {
        DiscoveryStatsMenu menu = new DiscoveryStatsMenu();
        assertNotNull(menu);
    }
}
