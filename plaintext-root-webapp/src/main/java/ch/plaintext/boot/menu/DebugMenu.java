package ch.plaintext.boot.menu;

@MenuAnnotation(
    title = "Debug",
    link = "debug.html",
    parent = "Admin",
    order = 50,
    icon = "pi pi-wrench",
    roles = {"ADMIN"}
)
public class DebugMenu {

}
