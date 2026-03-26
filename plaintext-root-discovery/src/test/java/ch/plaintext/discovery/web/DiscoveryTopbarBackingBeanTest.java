/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.web;

import ch.plaintext.boot.plugins.security.PlaintextSecurityHolder;
import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import ch.plaintext.discovery.service.DiscoveryEncryptionService;
import ch.plaintext.discovery.service.DiscoveryService;
import ch.plaintext.discovery.web.DiscoveryTopbarBackingBean.RemoteAppItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    private MockedStatic<PlaintextSecurityHolder> securityHolderMock;

    @BeforeEach
    void setUp() {
        bean = new DiscoveryTopbarBackingBean(
            discoveryService, sessionRepository, appRepository, encryptionService);
        ReflectionTestUtils.setField(bean, "appId", "test-app");
    }

    @AfterEach
    void tearDown() {
        if (securityHolderMock != null) {
            securityHolderMock.close();
            securityHolderMock = null;
        }
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
    class LoadRemoteApps {

        @Test
        void loadsRemoteAppsWhenUserIsFound() {
            securityHolderMock = mockStatic(PlaintextSecurityHolder.class);
            securityHolderMock.when(PlaintextSecurityHolder::getUser).thenReturn("user@test.com");

            DiscoveryApp app1 = createApp("remote-1", "Remote One", "http://remote1:8080", DiscoveryApp.AppEnvironment.PROD);
            DiscoveryApp app2 = createApp("remote-2", "Remote Two", "http://remote2:8080", DiscoveryApp.AppEnvironment.DEV);

            DiscoveryUserSession session1 = createSession(app1, "user@test.com");
            DiscoveryUserSession session2 = createSession(app2, "user@test.com");

            when(discoveryService.getRemoteAppsForUser("user@test.com"))
                .thenReturn(List.of(session1, session2));

            bean.loadRemoteApps();

            assertNotNull(bean.getRemoteApps());
            assertEquals(2, bean.getRemoteApps().size());
            assertEquals("remote-1", bean.getRemoteApps().get(0).getAppId());
            assertEquals("Remote One", bean.getRemoteApps().get(0).getAppName());
            assertEquals("http://remote1:8080", bean.getRemoteApps().get(0).getAppUrl());
            assertEquals("PROD", bean.getRemoteApps().get(0).getEnvironment());
            assertEquals("pi pi-server", bean.getRemoteApps().get(0).getIcon());

            assertEquals("remote-2", bean.getRemoteApps().get(1).getAppId());
            assertEquals("pi pi-wrench", bean.getRemoteApps().get(1).getIcon());
        }

        @Test
        void doesNotLoadWhenUserIsNull() {
            securityHolderMock = mockStatic(PlaintextSecurityHolder.class);
            securityHolderMock.when(PlaintextSecurityHolder::getUser).thenReturn(null);

            bean.loadRemoteApps();

            assertNull(bean.getRemoteApps());
            verifyNoInteractions(discoveryService);
        }

        @Test
        void handlesExceptionGracefully() {
            securityHolderMock = mockStatic(PlaintextSecurityHolder.class);
            securityHolderMock.when(PlaintextSecurityHolder::getUser)
                .thenThrow(new RuntimeException("Security context not available"));

            bean.loadRemoteApps();

            // Should not throw, remoteApps stays null
            assertNull(bean.getRemoteApps());
        }

        @Test
        void loadsEmptyListWhenNoSessions() {
            securityHolderMock = mockStatic(PlaintextSecurityHolder.class);
            securityHolderMock.when(PlaintextSecurityHolder::getUser).thenReturn("user@test.com");

            when(discoveryService.getRemoteAppsForUser("user@test.com"))
                .thenReturn(List.of());

            bean.loadRemoteApps();

            assertNotNull(bean.getRemoteApps());
            assertTrue(bean.getRemoteApps().isEmpty());
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

        @Test
        void returnsFullLoginUrlWhenAllServicesAvailable() {
            securityHolderMock = mockStatic(PlaintextSecurityHolder.class);
            securityHolderMock.when(PlaintextSecurityHolder::getUser).thenReturn("user@test.com");

            DiscoveryApp targetApp = createApp("remote-app", "Remote App", "http://remote:8080", DiscoveryApp.AppEnvironment.PROD);
            targetApp.setPublicKey("BASE64_PUBLIC_KEY");

            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.of(targetApp));
            when(encryptionService.createDiscoveryToken("user@test.com", "test-app", "BASE64_PUBLIC_KEY"))
                .thenReturn("encrypted-token-value");

            RemoteAppItem item = new RemoteAppItem();
            item.setAppId("remote-app");
            item.setAppName("Remote App");
            item.setAppUrl("http://remote:8080");

            String url = bean.getLoginUrl(item);

            assertTrue(url.startsWith("http://remote:8080/discovery/login?token="));
            assertTrue(url.contains("encrypted-token-value"));
        }

        @Test
        void returnsFallbackUrlWhenTargetAppHasNoPublicKey() {
            securityHolderMock = mockStatic(PlaintextSecurityHolder.class);
            securityHolderMock.when(PlaintextSecurityHolder::getUser).thenReturn("user@test.com");

            DiscoveryApp targetApp = createApp("remote-app", "Remote App", "http://remote:8080", DiscoveryApp.AppEnvironment.PROD);
            targetApp.setPublicKey(null); // no public key

            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.of(targetApp));

            RemoteAppItem item = new RemoteAppItem();
            item.setAppId("remote-app");
            item.setAppName("Remote App");
            item.setAppUrl("http://remote:8080");

            String url = bean.getLoginUrl(item);

            assertEquals("http://remote:8080", url);
            verifyNoInteractions(encryptionService);
        }

        @Test
        void returnsFallbackUrlWhenTargetAppNotFound() {
            securityHolderMock = mockStatic(PlaintextSecurityHolder.class);
            securityHolderMock.when(PlaintextSecurityHolder::getUser).thenReturn("user@test.com");

            when(appRepository.findByAppId("unknown-app")).thenReturn(Optional.empty());

            RemoteAppItem item = new RemoteAppItem();
            item.setAppId("unknown-app");
            item.setAppName("Unknown App");
            item.setAppUrl("http://unknown:8080");

            String url = bean.getLoginUrl(item);

            assertEquals("http://unknown:8080", url);
            verifyNoInteractions(encryptionService);
        }

        @Test
        void returnsFallbackUrlWhenUserEmailIsNull() {
            securityHolderMock = mockStatic(PlaintextSecurityHolder.class);
            securityHolderMock.when(PlaintextSecurityHolder::getUser).thenReturn(null);

            RemoteAppItem item = new RemoteAppItem();
            item.setAppId("remote-app");
            item.setAppName("Remote App");
            item.setAppUrl("http://remote:8080");

            String url = bean.getLoginUrl(item);

            assertEquals("http://remote:8080", url);
            verifyNoInteractions(appRepository);
            verifyNoInteractions(encryptionService);
        }

        @Test
        void urlEncodesTokenValue() {
            securityHolderMock = mockStatic(PlaintextSecurityHolder.class);
            securityHolderMock.when(PlaintextSecurityHolder::getUser).thenReturn("user@test.com");

            DiscoveryApp targetApp = createApp("remote-app", "Remote App", "http://remote:8080", DiscoveryApp.AppEnvironment.PROD);
            targetApp.setPublicKey("BASE64_PUBLIC_KEY");

            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.of(targetApp));
            // Token with special characters that need URL encoding
            when(encryptionService.createDiscoveryToken("user@test.com", "test-app", "BASE64_PUBLIC_KEY"))
                .thenReturn("token+with/special=chars");

            RemoteAppItem item = new RemoteAppItem();
            item.setAppId("remote-app");
            item.setAppName("Remote App");
            item.setAppUrl("http://remote:8080");

            String url = bean.getLoginUrl(item);

            // The + should be encoded as %2B, / as %2F, = as %3D
            assertFalse(url.contains("token+with/special=chars"));
            assertTrue(url.contains("token%2Bwith%2Fspecial%3Dchars"));
        }

        @Test
        void returnsFallbackUrlWhenEncryptionServiceThrows() {
            securityHolderMock = mockStatic(PlaintextSecurityHolder.class);
            securityHolderMock.when(PlaintextSecurityHolder::getUser).thenReturn("user@test.com");

            DiscoveryApp targetApp = createApp("remote-app", "Remote App", "http://remote:8080", DiscoveryApp.AppEnvironment.PROD);
            targetApp.setPublicKey("INVALID_KEY");

            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.of(targetApp));
            when(encryptionService.createDiscoveryToken(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Encryption failed"));

            RemoteAppItem item = new RemoteAppItem();
            item.setAppId("remote-app");
            item.setAppName("Remote App");
            item.setAppUrl("http://remote:8080");

            String url = bean.getLoginUrl(item);

            assertEquals("http://remote:8080", url);
        }
    }

    @Nested
    class ResolveServiceTest {

        @Test
        void returnsServiceDirectlyWhenNotNull() throws Exception {
            // Use reflection to call private resolveService method
            java.lang.reflect.Method method = DiscoveryTopbarBackingBean.class
                .getDeclaredMethod("resolveService", Object.class, Class.class);
            method.setAccessible(true);

            DiscoveryEncryptionService result = (DiscoveryEncryptionService)
                method.invoke(bean, encryptionService, DiscoveryEncryptionService.class);

            assertSame(encryptionService, result);
        }

        @Test
        void resolvesFromContextWhenServiceIsNull() throws Exception {
            java.lang.reflect.Method method = DiscoveryTopbarBackingBean.class
                .getDeclaredMethod("resolveService", Object.class, Class.class);
            method.setAccessible(true);

            // Set context to null so getBean returns null
            ch.plaintext.discovery.config.DiscoveryContextHolder holder =
                new ch.plaintext.discovery.config.DiscoveryContextHolder();
            holder.setApplicationContext(null);

            Object result = method.invoke(bean, null, DiscoveryEncryptionService.class);

            assertNull(result);
        }
    }

    @Nested
    class IconForEnvironment {

        @Test
        void prodReturnsServerIcon() {
            DiscoveryApp app = createApp("test", "Test", "http://test:8080", DiscoveryApp.AppEnvironment.PROD);
            DiscoveryUserSession session = createSession(app, "user@test.com");

            RemoteAppItem item = callConvertToRemoteAppItem(session);
            assertEquals("pi pi-server", item.getIcon());
        }

        @Test
        void devReturnsWrenchIcon() {
            DiscoveryApp app = createApp("test", "Test", "http://test:8080", DiscoveryApp.AppEnvironment.DEV);
            RemoteAppItem item = callConvertToRemoteAppItem(createSession(app, "user@test.com"));
            assertEquals("pi pi-wrench", item.getIcon());
        }

        @Test
        void intReturnsCogIcon() {
            DiscoveryApp app = createApp("test", "Test", "http://test:8080", DiscoveryApp.AppEnvironment.INT);
            RemoteAppItem item = callConvertToRemoteAppItem(createSession(app, "user@test.com"));
            assertEquals("pi pi-cog", item.getIcon());
        }

        @Test
        void testReturnsVerifiedIcon() {
            DiscoveryApp app = createApp("test", "Test", "http://test:8080", DiscoveryApp.AppEnvironment.TEST);
            RemoteAppItem item = callConvertToRemoteAppItem(createSession(app, "user@test.com"));
            assertEquals("pi pi-verified", item.getIcon());
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

    @Nested
    class ConvertToRemoteAppItemTest {

        @Test
        void mapsAllFieldsCorrectly() {
            DiscoveryApp app = createApp("my-app", "My Application", "http://myapp:9090", DiscoveryApp.AppEnvironment.INT);
            DiscoveryUserSession session = createSession(app, "admin@example.com");

            RemoteAppItem item = callConvertToRemoteAppItem(session);

            assertEquals("my-app", item.getAppId());
            assertEquals("My Application", item.getAppName());
            assertEquals("http://myapp:9090", item.getAppUrl());
            assertEquals("INT", item.getEnvironment());
            assertEquals("pi pi-cog", item.getIcon());
        }

        private RemoteAppItem callConvertToRemoteAppItem(DiscoveryUserSession session) {
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

    // --- Helper methods ---

    private static DiscoveryApp createApp(String appId, String appName, String appUrl, DiscoveryApp.AppEnvironment env) {
        DiscoveryApp app = new DiscoveryApp();
        app.setAppId(appId);
        app.setAppName(appName);
        app.setAppUrl(appUrl);
        app.setEnvironment(env);
        return app;
    }

    private static DiscoveryUserSession createSession(DiscoveryApp app, String userEmail) {
        DiscoveryUserSession session = new DiscoveryUserSession();
        session.setApp(app);
        session.setUserEmail(userEmail);
        return session;
    }
}
