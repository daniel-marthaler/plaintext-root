/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.anforderungen.menu;

import ch.plaintext.boot.menu.MenuAnnotation;

@MenuAnnotation(
    title = "Howtos",
    link = "howtos.html",
    parent = "Anforderungen",
    order = 2,
    icon = "pi pi-book"
)
public class HowtosSubmenu {
}
