/*
 * Copyright (C) plaintext.ch, 2024.
 */
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
