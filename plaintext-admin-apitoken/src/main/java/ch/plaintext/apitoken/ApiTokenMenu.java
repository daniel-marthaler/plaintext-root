/*
 * Copyright (C) eMad, 2026.
 */
package ch.plaintext.apitoken;

import ch.plaintext.boot.menu.MenuAnnotation;

/**
 * Menu entry for API Token management
 *
 * @author info@emad.ch
 * @since 2026
 */
@MenuAnnotation(
    title = "API Token",
    link = "api-token.html",
    order = 90,
    parent = "Admin",
    icon = "pi pi-key",
    roles = {"USER", "ADMIN", "ROOT"}
)
public class ApiTokenMenu {
}
