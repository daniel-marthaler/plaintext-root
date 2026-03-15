/*
 * Copyright (C) plaintext.ch, 2024.
 */
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
