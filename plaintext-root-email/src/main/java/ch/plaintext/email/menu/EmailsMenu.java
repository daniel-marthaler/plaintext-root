/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.menu;

import ch.plaintext.boot.menu.MenuAnnotation;

/**
 * Emails Menu
 *
 * @author info@plaintext.ch
 * @since 2024
 */
@MenuAnnotation(
    title = "Emails",
    link = "emails.html",
    parent = "Admin",
    order = 10,
    icon = "pi pi-envelope",
    roles = {"ADMIN", "ROOT"}
)
public class EmailsMenu {

}
