/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.settings.menu;

import ch.plaintext.boot.menu.MenuAnnotation;

@MenuAnnotation(
    title = "Setup",
    link = "setup.html",
    parent = "Root",
    order = 2,
    icon = "pi pi-cog"
)
public class SetupSubmenu {
}
