package ch.plaintext.flyway;

import ch.plaintext.boot.menu.MenuAnnotation;

/**
 * Flyway Menu
 *
 * @author plaintext.ch
 * @since 1.108.0
 */
@MenuAnnotation(
    title = "Flyway",
    link = "flyway.html",
    parent = "Root",
    order = 100,
    icon = "pi pi-database",
    roles = {"ROOT"}
)
public class FlywayMenu {

}
