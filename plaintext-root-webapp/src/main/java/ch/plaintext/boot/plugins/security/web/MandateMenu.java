/*
 * Copyright (C) plaintext.ch, 2024.
 */
package ch.plaintext.boot.plugins.security.web;

import ch.plaintext.boot.menu.MenuAnnotation;

/**
 * Mandate Menu
 *
 * @author info@plaintext.ch
 * @since 2024
 */
@MenuAnnotation(
    title = "Mandate",
    link = "mandate.html",
    parent = "Root",
    order = 61,
    icon = "pi pi-list",
    roles = {"ROOT"}
)
public class MandateMenu {

}
