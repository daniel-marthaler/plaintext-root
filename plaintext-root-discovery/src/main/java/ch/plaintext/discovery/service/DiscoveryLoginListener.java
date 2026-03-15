package ch.plaintext.discovery.service;

import ch.plaintext.boot.plugins.security.PlaintextLoginEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for user login events and triggers discovery announcement
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DiscoveryLoginListener {

    private final DiscoveryService discoveryService;

    @EventListener
    public void handleUserLogin(PlaintextLoginEvent event) {
        try {
            if (event.getRequestBaseUrl() != null) {
                discoveryService.updateAppUrlFromRequest(event.getRequestBaseUrl());
            }
            discoveryService.announceUserLogin(
                event.getUserEmail(),
                event.getUserId(),
                event.getUserName()
            );
            log.debug("Discovery announcement sent for user login: {}", event.getUserEmail());
        } catch (Exception e) {
            log.error("Error handling discovery login event for user: {}", event.getUserEmail(), e);
        }
    }
}
