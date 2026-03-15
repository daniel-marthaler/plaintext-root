package ch.plaintext.boot.performance;

import ch.plaintext.boot.menu.MenuAnnotation;

/**
 * Performance menu item
 */
@MenuAnnotation(
    title = "Performance",
    link = "performance.html",
    parent = "Root",
    order = 1,
    icon = "pi pi-prime",
    roles = {"ROOT"}
)
public class PerformanceMenu {

}
