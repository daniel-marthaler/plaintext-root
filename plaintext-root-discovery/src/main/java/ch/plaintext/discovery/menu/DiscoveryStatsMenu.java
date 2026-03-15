package ch.plaintext.discovery.menu;

import ch.plaintext.boot.menu.MenuAnnotation;

/**
 * Discovery statistics menu (only visible to ROOT users)
 */
@MenuAnnotation(
    title = "Discovery Stats", 
    link = "discoveryStats.html",
    parent = "Admin",
    order = 50,
    icon = "pi pi-chart-line",
    roles = {"ROOT"}
)
public class DiscoveryStatsMenu {
}