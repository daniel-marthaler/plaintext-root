/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.wertelisten.menu;

import ch.plaintext.boot.menu.MenuAnnotation;

@MenuAnnotation(
    title = "Wertelisten",
    link = "wertelisten.html",
    parent = "Admin",
    order = 3,
    icon = "pi pi-list"
)
public class WertelistenSubmenu {
}
