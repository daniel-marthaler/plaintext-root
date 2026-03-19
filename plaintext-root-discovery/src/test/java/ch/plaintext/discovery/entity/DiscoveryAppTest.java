/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.entity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryAppTest {

    @Nested
    class FieldDefaults {

        @Test
        void activeDefaultsToTrue() {
            DiscoveryApp app = new DiscoveryApp();
            assertTrue(app.getActive());
        }

        @Test
        void fieldsAreInitiallyNull() {
            DiscoveryApp app = new DiscoveryApp();
            assertNull(app.getAppId());
            assertNull(app.getAppName());
            assertNull(app.getAppUrl());
            assertNull(app.getEnvironment());
            assertNull(app.getPublicKey());
            assertNull(app.getLastSeenAt());
            assertNull(app.getVersion());
        }
    }

    @Nested
    class SettersAndGetters {

        @Test
        void setAndGetAllFields() {
            DiscoveryApp app = new DiscoveryApp();
            LocalDateTime now = LocalDateTime.now();

            app.setAppId("my-app");
            app.setAppName("My Application");
            app.setAppUrl("http://localhost:8080");
            app.setEnvironment(DiscoveryApp.AppEnvironment.PROD);
            app.setPublicKey("base64-encoded-key");
            app.setLastSeenAt(now);
            app.setVersion("2.1.0");
            app.setActive(false);

            assertEquals("my-app", app.getAppId());
            assertEquals("My Application", app.getAppName());
            assertEquals("http://localhost:8080", app.getAppUrl());
            assertEquals(DiscoveryApp.AppEnvironment.PROD, app.getEnvironment());
            assertEquals("base64-encoded-key", app.getPublicKey());
            assertEquals(now, app.getLastSeenAt());
            assertEquals("2.1.0", app.getVersion());
            assertFalse(app.getActive());
        }
    }

    @Nested
    class AppEnvironmentEnum {

        @Test
        void allEnvironmentValuesExist() {
            DiscoveryApp.AppEnvironment[] values = DiscoveryApp.AppEnvironment.values();

            assertEquals(4, values.length);
            assertNotNull(DiscoveryApp.AppEnvironment.PROD);
            assertNotNull(DiscoveryApp.AppEnvironment.DEV);
            assertNotNull(DiscoveryApp.AppEnvironment.INT);
            assertNotNull(DiscoveryApp.AppEnvironment.TEST);
        }

        @Test
        void valueOfWorksForAllEnvironments() {
            assertEquals(DiscoveryApp.AppEnvironment.PROD, DiscoveryApp.AppEnvironment.valueOf("PROD"));
            assertEquals(DiscoveryApp.AppEnvironment.DEV, DiscoveryApp.AppEnvironment.valueOf("DEV"));
            assertEquals(DiscoveryApp.AppEnvironment.INT, DiscoveryApp.AppEnvironment.valueOf("INT"));
            assertEquals(DiscoveryApp.AppEnvironment.TEST, DiscoveryApp.AppEnvironment.valueOf("TEST"));
        }

        @Test
        void valueOfThrowsForInvalidEnvironment() {
            assertThrows(IllegalArgumentException.class,
                () -> DiscoveryApp.AppEnvironment.valueOf("STAGING"));
        }
    }

    @Nested
    class InheritedSuperModelFields {

        @Test
        void idFieldIsAccessible() {
            DiscoveryApp app = new DiscoveryApp();
            app.setId(42L);
            assertEquals(42L, app.getId());
        }

        @Test
        void deletedFieldIsAccessible() {
            DiscoveryApp app = new DiscoveryApp();
            app.setDeleted(true);
            assertTrue(app.getDeleted());
        }

        @Test
        void mandatFieldIsAccessible() {
            DiscoveryApp app = new DiscoveryApp();
            app.setMandat("test-mandat");
            assertEquals("test-mandat", app.getMandat());
        }
    }
}
