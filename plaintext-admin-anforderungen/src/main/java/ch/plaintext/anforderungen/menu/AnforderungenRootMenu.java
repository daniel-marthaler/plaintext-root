package ch.plaintext.anforderungen.menu;

import ch.plaintext.boot.menu.MenuAnnotation;

@MenuAnnotation(
    title = "Anforderungen",
    order = 100,
    icon = "pi pi-list-check",
    roles = {"ADMIN", "ROOT"}
)
public class AnforderungenRootMenu {
}
