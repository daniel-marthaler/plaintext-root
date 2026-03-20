/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.web;

import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import ch.plaintext.discovery.service.DiscoveryEncryptionService;
import ch.plaintext.discovery.service.DiscoveryService;
import ch.plaintext.discovery.web.DiscoveryTopbarBackingBean.RemoteAppItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryTopbarBackingBeanTest {

    @Mock
    private DiscoveryService discoveryService;

    @Mock
    private DiscoveryUserSessionRepository sessionRepository;

    @Mock
    private DiscoveryAppRepository appRepository;

    @Mock
    private DiscoveryEncryptionService encryptionService;

    private DiscoveryTopbarBackingBean bean;

    @BeforeEach
    void setUp() {
        bean = new DiscoveryTopbarBackingBean(
            discoveryService, sessionRepository, appRepository, encryptionService);
        ReflectionTestUtils.setField(bean, "appId", "test-app");
    }

    @Nested
    class HasRemoteApps {

        @Test
        void returnsFalseWhenNull() {
            bean.setRemoteApps(null);
            assertFalse(bean.hasRemoteApps());
        }

        @Test
        void returnsFalseWhenEmpty() {
            bean.setRemoteApps(List.of());
            assertFalse(bean.hasRemoteApps());
        }

        @Test
        void returnsTrueWhenPresent() {
            bean.setRemoteApps(List.of(new RemoteAppItem()));
            assertTrue(bean.hasRemoteApps());
        }
    }

    @Nested
    class RemoteAppItemTest {

        @Test
        void allFieldsAreAccessible() {
            RemoteAppItem item = new RemoteAppItem();
            item.setAppId("app1");
            item.setAppName("App One");
            item.setAppUrl("http://app1:8080");
            item.setEnvironment("PROD");
            item.setIcon("pi pi-server");

            assertEquals("app1", item.getAppId());
            assertEquals("App One", item.getAppName());
            assertEquals("http://app1:8080", item.getAppUrl());
            assertEquals("PROD", item.getEnvironment());
            assertEquals("pi pi-server", item.getIcon());
        }
    }

    @Nested
    class GetLoginUrl {

        @Test
        void returnsFallbackUrlWhenEncryptionServiceNull() {
            // Simulate deserialized state (transient services are null)
            DiscoveryTopbarBackingBean deserializedBean = new DiscoveryTopbarBackingBean(
                null, null, null, null);
            ReflectionTestUtils.setField(deserializedBean, "appId", "test-app");

            // Set DiscoveryContextHolder to null to simulate no context
            ch.plaintext.discovery.config.DiscoveryContextHolder holder =
                new ch.plaintext.discovery.config.DiscoveryContextHolder();
            holder.setApplicationContext(null);

            RemoteAppItem item = new RemoteAppItem();
            item.setAppId("remote");
            item.setAppName("Remote");
            item.setAppUrl("http://remote:8080");

            String url = deserializedBean.getLoginUrl(item);

            assertEquals("http://remote:8080", url);
        }

        @Test
        void returnsFallbackUrlOnException() {
            RemoteAppItem item = new RemoteAppItem();
            item.setAppId("remote");
            item.setAppName("Remote");
            item.setAppUrl("http://remote:8080");

            // Mock PlaintextSecurityHolder.getUser() to throw
            // Since it's static, it will fail and be caught
            String url = bean.getLoginUrl(item);

            // Should fall back to plain URL because PlaintextSecurityHolder.getUser() will fail
            assertEquals("http://remote:8080", url);
        }
    }

    @Nested
    class IconForEnvironment {

        @Test
        void prodReturnsServerIcon() {
            DiscoveryApp app = createAppWithEnvironment(DiscoveryApp.AppEnvironment.PROD);
            DiscoveryUserSession session = createSession(app);

            List<DiscoveryUserSession> sessions = List.of(session);
            // Use reflection to call convertToRemoteAppItem via loadRemoteApps
            // Instead, test getIconForEnvironment indirectly via the bean
            // We can test it by checking the remote app items after setting them

            RemoteAppItem item = callConvertToRemoteAppItem(session);
            assertEquals("pi pi-server", item.getIcon());
        }

        @Test
        void devReturnsWrenchIcon() {
            DiscoveryApp app = createAppWithEnvironment(DiscoveryApp.AppEnvironment.DEV);
            RemoteAppItem item = callConvertToRemoteAppItem(createSession(app));
            assertEquals("pi pi-wrench", item.getIcon());
        }

        @Test
        void intReturnsCogIcon() {
            DiscoveryApp app = createAppWithEnvironment(DiscoveryApp.AppEnvironment.INT);
            RemoteAppItem item = callConvertToRemoteAppItem(createSession(app));
            assertEquals("pi pi-cog", item.getIcon());
        }

        @Test
        void testReturnsVerifiedIcon() {
            DiscoveryApp app = createAppWithEnvironment(DiscoveryApp.AppEnvironment.TEST);
            RemoteAppItem item = callConvertToRemoteAppItem(createSession(app));
            assertEquals("pi pi-verified", item.getIcon());
        }

        private DiscoveryApp createAppWithEnvironment(DiscoveryApp.AppEnvironment env) {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppId("test");
            app.setAppName("Test");
            app.setAppUrl("http://test:8080");
            app.setEnvironment(env);
            return app;
        }

        private DiscoveryUserSession createSession(DiscoveryApp app) {
            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setApp(app);
            session.setUserEmail("user@test.com");
            return session;
        }

        private RemoteAppItem callConvertToRemoteAppItem(DiscoveryUserSession session) {
            // Use reflection to call private method
            try {
                java.lang.reflect.Method method = DiscoveryTopbarBackingBean.class
                    .getDeclaredMethod("convertToRemoteAppItem", DiscoveryUserSession.class);
                method.setAccessible(true);
                return (RemoteAppItem) method.invoke(bean, session);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
