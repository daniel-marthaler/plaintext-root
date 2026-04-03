/*
 * Copyright (C) eMad, 2026.
 */
package ch.plaintext.apitoken;

import ch.plaintext.boot.menu.MenuAnnotation;

/**
 * Admin menu entry for API Token management (all tokens in mandat).
 *
 * @author info@emad.ch
 * @since 2026
 */
@MenuAnnotation(
    title = "API Tokens",
    link = "admin-api-token.html",
    order = 30,
    parent = "Admin",
    icon = "pi pi-key",
    roles = {"ADMIN", "ROOT"}
)
public class AdminApiTokenMenu {
}
