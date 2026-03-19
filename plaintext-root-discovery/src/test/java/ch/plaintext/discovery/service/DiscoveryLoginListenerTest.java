/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.service;

import ch.plaintext.boot.plugins.security.PlaintextLoginEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryLoginListenerTest {

    @Mock
    private DiscoveryService discoveryService;

    @InjectMocks
    private DiscoveryLoginListener loginListener;

    @Test
    void handleUserLoginAnnouncesLogin() {
        PlaintextLoginEvent event = new PlaintextLoginEvent(
            this, "user@test.com", 42L, "Test User", "test-mandat");

        loginListener.handleUserLogin(event);

        verify(discoveryService).announceUserLogin("user@test.com", 42L, "Test User");
    }

    @Test
    void handleUserLoginUpdatesAppUrlWhenProvided() {
        PlaintextLoginEvent event = new PlaintextLoginEvent(
            this, "user@test.com", 42L, "Test User", "test-mandat", "https://myapp.example.com");

        loginListener.handleUserLogin(event);

        verify(discoveryService).updateAppUrlFromRequest("https://myapp.example.com");
        verify(discoveryService).announceUserLogin("user@test.com", 42L, "Test User");
    }

    @Test
    void handleUserLoginDoesNotUpdateUrlWhenNull() {
        PlaintextLoginEvent event = new PlaintextLoginEvent(
            this, "user@test.com", 42L, "Test User", "test-mandat", null);

        loginListener.handleUserLogin(event);

        verify(discoveryService, never()).updateAppUrlFromRequest(any());
        verify(discoveryService).announceUserLogin("user@test.com", 42L, "Test User");
    }

    @Test
    void handleUserLoginHandlesExceptionGracefully() {
        PlaintextLoginEvent event = new PlaintextLoginEvent(
            this, "user@test.com", 42L, "Test User", "test-mandat");

        doThrow(new RuntimeException("Error")).when(discoveryService)
            .announceUserLogin(any(), any(), any());

        // Should not throw
        loginListener.handleUserLogin(event);
    }
}
