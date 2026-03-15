package ch.plaintext.cron;

import ch.plaintext.boot.menu.MenuAnnotation;

@MenuAnnotation(
    title = "Cron",
    link = "cron.html",
    order = 10,
    parent = "Admin",
    icon = "pi pi-calendar-times",
    roles = {"ADMIN"}
)
public class CronMenu {

}
