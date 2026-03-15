package ch.plaintext.discovery.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Static holder for Spring ApplicationContext.
 * Used by session-scoped beans to re-resolve transient services after session deserialization
 * (e.g., during blue-green deployment session transfer).
 */
@Component
public class DiscoveryContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> clazz) {
        return context != null ? context.getBean(clazz) : null;
    }
}
