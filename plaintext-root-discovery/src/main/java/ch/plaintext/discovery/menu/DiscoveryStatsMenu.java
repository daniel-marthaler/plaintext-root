/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.menu;

import ch.plaintext.boot.menu.MenuAnnotation;

/**
 * Discovery statistics menu (only visible to ROOT users)
 */
@MenuAnnotation(
    title = "Discovery Stats", 
    link = "discoveryStats.html",
    parent = "Admin",
    order = 50,
    icon = "pi pi-chart-line",
    roles = {"ROOT"}
)
public class DiscoveryStatsMenu {
}