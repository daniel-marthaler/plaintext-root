package ch.plaintext.boot.menu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark classes as menu items.
 * The annotated class will be automatically discovered and added to the menu.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MenuAnnotation {

    /**
     * The title/label of the menu item
     */
    String title() default "Dashboard";

    /**
     * The link/URL of the menu item
     */
    String link() default "dashboard.html";

    /**
     * The parent menu item (empty for root menu items)
     */
    String parent() default "";

    /**
     * The order of the menu item (lower numbers appear first)
     */
    int order() default 0;

    /**
     * The icon for the menu item (PrimeFaces icon class)
     */
    String icon() default "";

    /**
     * The roles that can see this menu item (empty means visible to all)
     */
    String[] roles() default {};

}
