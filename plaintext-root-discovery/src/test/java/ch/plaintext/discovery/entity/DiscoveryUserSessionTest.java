/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.entity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryUserSessionTest {

    @Nested
    class FieldDefaults {

        @Test
        void sessionActiveDefaultsToTrue() {
            DiscoveryUserSession session = new DiscoveryUserSession();
            assertTrue(session.getSessionActive());
        }

        @Test
        void tokenUsedDefaultsToFalse() {
            DiscoveryUserSession session = new DiscoveryUserSession();
            assertFalse(session.getTokenUsed());
        }

        @Test
        void optionalFieldsAreInitiallyNull() {
            DiscoveryUserSession session = new DiscoveryUserSession();
            assertNull(session.getApp());
            assertNull(session.getUserEmail());
            assertNull(session.getUserId());
            assertNull(session.getUserName());
            assertNull(session.getLoggedInAt());
            assertNull(session.getLastActivityAt());
            assertNull(session.getLoginToken());
            assertNull(session.getTokenExpiresAt());
        }
    }

    @Nested
    class SettersAndGetters {

        @Test
        void setAndGetAllFields() {
            DiscoveryUserSession session = new DiscoveryUserSession();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expires = now.plusMinutes(5);

            DiscoveryApp app = new DiscoveryApp();
            app.setAppId("test-app");

            session.setApp(app);
            session.setUserEmail("user@example.com");
            session.setUserId(42L);
            session.setUserName("Test User");
            session.setLoggedInAt(now);
            session.setLastActivityAt(now);
            session.setSessionActive(false);
            session.setLoginToken("token-uuid");
            session.setTokenExpiresAt(expires);
            session.setTokenUsed(true);

            assertEquals(app, session.getApp());
            assertEquals("user@example.com", session.getUserEmail());
            assertEquals(42L, session.getUserId());
            assertEquals("Test User", session.getUserName());
            assertEquals(now, session.getLoggedInAt());
            assertEquals(now, session.getLastActivityAt());
            assertFalse(session.getSessionActive());
            assertEquals("token-uuid", session.getLoginToken());
            assertEquals(expires, session.getTokenExpiresAt());
            assertTrue(session.getTokenUsed());
        }
    }

    @Nested
    class AppRelationship {

        @Test
        void canSetAndRetrieveApp() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppId("remote-app");
            app.setAppName("Remote Application");
            app.setAppUrl("http://remote:8080");
            app.setEnvironment(DiscoveryApp.AppEnvironment.PROD);

            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setApp(app);

            assertEquals("remote-app", session.getApp().getAppId());
            assertEquals("Remote Application", session.getApp().getAppName());
            assertEquals(DiscoveryApp.AppEnvironment.PROD, session.getApp().getEnvironment());
        }
    }

    @Nested
    class TokenManagement {

        @Test
        void tokenCanBeSetAndConsumed() {
            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setLoginToken("abc-123");
            session.setTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
            session.setTokenUsed(false);

            assertFalse(session.getTokenUsed());
            assertEquals("abc-123", session.getLoginToken());

            // Consume the token
            session.setTokenUsed(true);
            assertTrue(session.getTokenUsed());
        }

        @Test
        void tokenExpirationCanBeChecked() {
            DiscoveryUserSession session = new DiscoveryUserSession();

            // Future expiration
            session.setTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
            assertTrue(session.getTokenExpiresAt().isAfter(LocalDateTime.now()));

            // Past expiration
            session.setTokenExpiresAt(LocalDateTime.now().minusMinutes(5));
            assertTrue(session.getTokenExpiresAt().isBefore(LocalDateTime.now()));
        }
    }

    @Nested
    class InheritedSuperModelFields {

        @Test
        void idFieldIsAccessible() {
            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setId(99L);
            assertEquals(99L, session.getId());
        }
    }
}
