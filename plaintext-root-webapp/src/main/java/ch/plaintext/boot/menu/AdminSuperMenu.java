package ch.plaintext.boot.menu;

/**
 * Home menu item
 */
@MenuAnnotation(
    title = "Admin",
    link = "index.html",
    parent = "",
    order = 2,
    icon = "pi pi-cog",
    roles = {"ADMIN"}
)
public class AdminSuperMenu {

}
