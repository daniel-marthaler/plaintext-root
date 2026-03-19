/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.menu;

import ch.plaintext.boot.menu.MenuAnnotation;

/**
 * Email Configuration Menu
 *
 * @author info@plaintext.ch
 * @since 2024
 */
@MenuAnnotation(
    title = "Email-Konfiguration",
    link = "emailconfig.html",
    parent = "Admin",
    order = 11,
    icon = "pi pi-cog",
    roles = {"ADMIN", "ROOT"}
)
public class EmailConfigMenu {

}
