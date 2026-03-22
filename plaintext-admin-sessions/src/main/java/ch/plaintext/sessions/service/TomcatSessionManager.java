/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.sessions.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Container;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Provides access to Tomcat's internal session Manager for creating
 * sessions with specific IDs (needed for session migration during blue-green deployments).
 *
 * <p>This bean is only activated when running inside a servlet web application.
 * In test environments with {@code WebEnvironment.MOCK} (no embedded server),
 * the bean will be created but methods will throw if the context is not a web server context.</p>
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Slf4j
public class TomcatSessionManager {

    private final ApplicationContext applicationContext;

    public TomcatSessionManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Check if the embedded Tomcat web server is available.
     * Returns false in test environments using WebEnvironment.MOCK.
     */
    public boolean isAvailable() {
        return applicationContext instanceof AnnotationConfigServletWebServerApplicationContext;
    }

    /**
     * Get the Tomcat session Manager.
     */
    private Manager getTomcatManager() {
        if (!(applicationContext instanceof AnnotationConfigServletWebServerApplicationContext webServerContext)) {
            throw new IllegalStateException("TomcatSessionManager requires an embedded web server context. "
                    + "Current context type: " + applicationContext.getClass().getSimpleName());
        }
        TomcatWebServer tomcatWebServer = (TomcatWebServer) webServerContext.getWebServer();
        Container[] containers = tomcatWebServer.getTomcat().getHost().findChildren();
        if (containers.length == 0) {
            throw new IllegalStateException("No Tomcat context found");
        }
        return ((StandardContext) containers[0]).getManager();
    }

    /**
     * Create a new HttpSession with a specific session ID.
     * This allows preserving session IDs during blue-green deployment session migration.
     *
     * @param sessionId The desired session ID
     * @return The created HttpSession
     */
    public HttpSession createSessionWithId(String sessionId) {
        Manager manager = getTomcatManager();

        // Remove existing session with this ID if present
        try {
            Session existing = manager.findSession(sessionId);
            if (existing != null) {
                existing.expire();
                log.debug("Expired existing session: {}", sessionId);
            }
        } catch (Exception e) {
            log.debug("No existing session to expire: {}", sessionId);
        }

        Session session = manager.createSession(sessionId);
        log.debug("Created Tomcat session with ID: {}", session.getId());
        return session.getSession();
    }
}
