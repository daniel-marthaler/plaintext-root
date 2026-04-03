/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.apitoken;

import ch.plaintext.boot.menu.MenuAnnotation;

/**
 * Root menu entry for API Token management (all tokens across all mandats).
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@MenuAnnotation(
    title = "API Tokens (Root)",
    link = "root-api-token.html",
    order = 120,
    parent = "Root",
    icon = "pi pi-key",
    roles = {"ROOT"}
)
public class RootApiTokenMenu {
}
