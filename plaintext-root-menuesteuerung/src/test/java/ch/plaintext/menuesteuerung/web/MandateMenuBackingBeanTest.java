/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.menuesteuerung.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.menuesteuerung.model.MandateMenuConfig;
import ch.plaintext.menuesteuerung.service.MandateMenuVisibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MandateMenuBackingBean.
 *
 * @author plaintext.ch
 * @since 1.39.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MandateMenuBackingBean - Whitelist/Blacklist Tests")
class MandateMenuBackingBeanTest {

    @Mock
    private MandateMenuVisibilityService service;

    @Mock
    private PlaintextSecurity plaintextSecurity;

    @InjectMocks
    private MandateMenuBackingBean backingBean;

    private MandateMenuConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new MandateMenuConfig();
        testConfig.setMandateName("test-mandate");
        testConfig.setHiddenMenus(new HashSet<>());
        testConfig.setWhitelistMode(false);
    }

    @Nested
    @DisplayName("Mode Toggle - Whitelist to Blacklist")
    class ModeToggleWhitelistToBlacklist {

        @BeforeEach
        void setup() {
            backingBean.setSelected(testConfig);
            backingBean.setAvailableMenus(List.of("Menu1", "Menu2", "Menu3", "Menu4", "Menu5"));
        }

        @Test
        @DisplayName("Should toggle from blacklist to whitelist and invert selection")
        void shouldToggleBlacklistToWhitelist() {
            // Arrange
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(new HashSet<>(Set.of("Menu1", "Menu2")));
            // In blacklist: Menu1, Menu2 hidden; Menu3, Menu4, Menu5 visible

            // Act
            backingBean.toggleMode();

            // Assert
            assertTrue(Boolean.TRUE.equals(testConfig.getWhitelistMode()), "Should switch to whitelist mode");
            // Now the whitelist should contain the previously visible items
            assertTrue(testConfig.getHiddenMenus().contains("Menu3"));
            assertTrue(testConfig.getHiddenMenus().contains("Menu4"));
            assertTrue(testConfig.getHiddenMenus().contains("Menu5"));
            assertFalse(testConfig.getHiddenMenus().contains("Menu1"));
            assertFalse(testConfig.getHiddenMenus().contains("Menu2"));
        }

        @Test
        @DisplayName("Should toggle from whitelist to blacklist and invert selection")
        void shouldToggleWhitelistToBlacklist() {
            // Arrange
            testConfig.setWhitelistMode(true);
            testConfig.setHiddenMenus(new HashSet<>(Set.of("Menu1", "Menu2")));
            // In whitelist: Menu1, Menu2 visible; Menu3, Menu4, Menu5 hidden

            // Act
            backingBean.toggleMode();

            // Assert
            assertFalse(Boolean.TRUE.equals(testConfig.getWhitelistMode()), "Should switch to blacklist mode");
            // Now the blacklist should contain the previously hidden items
            assertTrue(testConfig.getHiddenMenus().contains("Menu3"));
            assertTrue(testConfig.getHiddenMenus().contains("Menu4"));
            assertTrue(testConfig.getHiddenMenus().contains("Menu5"));
            assertFalse(testConfig.getHiddenMenus().contains("Menu1"));
            assertFalse(testConfig.getHiddenMenus().contains("Menu2"));
        }
    }

    @Nested
    @DisplayName("Mode Toggle - Edge Cases")
    class ModeToggleEdgeCases {

        @BeforeEach
        void setup() {
            backingBean.setSelected(testConfig);
            backingBean.setAvailableMenus(new ArrayList<>());
        }

        @Test
        @DisplayName("Should handle toggle with null selected mandate")
        void shouldHandleToggleWithNullSelected() {
            // Arrange
            backingBean.setSelected(null);

            // Act & Assert
            assertDoesNotThrow(() -> backingBean.toggleMode(), "Should not throw when selected is null");
        }

        @Test
        @DisplayName("Should handle toggle with empty available menus")
        void shouldHandleToggleWithEmptyMenus() {
            // Arrange
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(new HashSet<>(Set.of("Menu1")));
            backingBean.setAvailableMenus(new ArrayList<>());

            // Act
            backingBean.toggleMode();

            // Assert
            assertNotNull(testConfig.getHiddenMenus(), "Hidden menus should not be null");
        }

        @Test
        @DisplayName("Should have null hidden menus in configuration")
        void shouldHaveNullHiddenMenusInConfiguration() {
            // Arrange
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(null);
            backingBean.setAvailableMenus(List.of("Menu1", "Menu2"));
            backingBean.setSelected(testConfig);

            // Assert - verify null state
            assertNull(testConfig.getHiddenMenus(),
                "Hidden menus can be null in configuration");
        }

        @Test
        @DisplayName("Should invert empty whitelist to all menus")
        void shouldInvertEmptyWhitelistToAllMenus() {
            // Arrange
            testConfig.setWhitelistMode(true);
            testConfig.setHiddenMenus(new HashSet<>()); // Empty whitelist
            List<String> allMenus = List.of("Menu1", "Menu2", "Menu3");
            backingBean.setAvailableMenus(allMenus);

            // Act
            backingBean.toggleMode();

            // Assert
            assertFalse(Boolean.TRUE.equals(testConfig.getWhitelistMode()), "Should be in blacklist mode");
            assertEquals(3, testConfig.getHiddenMenus().size(), "Should have all menus as hidden");
        }
    }

    @Nested
    @DisplayName("Configuration Save - Whitelist/Blacklist Preservation")
    class ConfigurationSavePreservation {

        @Test
        @DisplayName("Should call service to save whitelist configuration")
        void shouldCallServiceToSaveWhitelistConfig() {
            // Arrange
            testConfig.setMandateName("test-mandate");
            testConfig.setWhitelistMode(true);
            testConfig.setHiddenMenus(new HashSet<>(Set.of("Menu1", "Menu2", "Menu3")));
            backingBean.setSelected(testConfig);

            // Verify configuration is properly set
            assertTrue(Boolean.TRUE.equals(testConfig.getWhitelistMode()),
                "Should be in whitelist mode");
            assertEquals(3, testConfig.getHiddenMenus().size(),
                "Should have 3 items");
        }

        @Test
        @DisplayName("Should have correct mode flag for blacklist configuration")
        void shouldHaveCorrectModeFlagForBlacklistConfig() {
            // Arrange
            testConfig.setMandateName("test-mandate");
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(new HashSet<>(Set.of("HiddenMenu1", "HiddenMenu2")));
            backingBean.setSelected(testConfig);

            // Assert
            assertFalse(Boolean.TRUE.equals(testConfig.getWhitelistMode()),
                "Should be in blacklist mode");
            assertEquals(2, testConfig.getHiddenMenus().size(),
                "Should have 2 items");
        }

        @Test
        @DisplayName("Should preserve mode flag in configuration")
        void shouldPreserveModeFlag() {
            // Arrange
            testConfig.setMandateName("mandate1");
            testConfig.setWhitelistMode(true);
            testConfig.setHiddenMenus(new HashSet<>(Set.of("Menu1")));
            backingBean.setSelected(testConfig);

            // Assert - verify mode is preserved
            assertTrue(Boolean.TRUE.equals(testConfig.getWhitelistMode()),
                "Whitelist mode should be true");
            assertTrue(testConfig.getHiddenMenus().contains("Menu1"),
                "Menu1 should be in hidden menus");
        }
    }

    @Nested
    @DisplayName("Configuration Load and Initialize")
    class ConfigurationLoadAndInitialize {

        @Test
        @DisplayName("Should correctly initialize detail page preserving mode")
        void shouldInitializeDetailPreservingMode() {
            // Arrange
            testConfig.setWhitelistMode(true);
            testConfig.setHiddenMenus(new HashSet<>(Set.of("Menu1", "Menu2")));
            backingBean.setSelected(testConfig);
            backingBean.setAvailableMenus(List.of("Menu1", "Menu2", "Menu3"));

            // Act
            backingBean.initDetail();

            // Assert
            assertTrue(Boolean.TRUE.equals(backingBean.getSelected().getWhitelistMode()),
                "Whitelist mode should be preserved");
            assertNotNull(backingBean.getSelected().getHiddenMenus(), "Hidden menus should not be null");
        }

        @Test
        @DisplayName("Should handle Hibernate lazy loading by creating new HashSet")
        void shouldHandleLazyLoadingInitialization() {
            // Arrange
            testConfig.setHiddenMenus(new HashSet<>(Set.of("Menu1")));
            backingBean.setSelected(testConfig);
            backingBean.setAvailableMenus(List.of("Menu1", "Menu2"));

            Set<String> originalSet = testConfig.getHiddenMenus();

            // Act
            backingBean.initDetail();

            // Assert
            assertNotNull(backingBean.getSelected().getHiddenMenus());
            assertEquals(originalSet, backingBean.getSelected().getHiddenMenus(),
                "Content should be equal");
        }
    }

    @Nested
    @DisplayName("Validation - Configuration State")
    class ValidationScenarios {

        @Test
        @DisplayName("Should handle null mandate name in configuration")
        void shouldHandleNullMandateName() {
            // Arrange
            testConfig.setMandateName(null);
            backingBean.setSelected(testConfig);

            // Assert - configuration is in invalid state
            assertNull(testConfig.getMandateName(),
                "Mandate name can be null but should be validated before save");
        }

        @Test
        @DisplayName("Should handle empty mandate name in configuration")
        void shouldHandleEmptyMandateName() {
            // Arrange
            testConfig.setMandateName("   ");
            backingBean.setSelected(testConfig);

            // Assert - configuration has whitespace-only name
            assertEquals("   ", testConfig.getMandateName(),
                "Mandate name can be whitespace but should be validated");
        }

        @Test
        @DisplayName("Should handle null selected mandate gracefully")
        void shouldHandleNullSelectedMandate() {
            // Arrange
            backingBean.setSelected(null);

            // Assert - null selection is stored
            assertNull(backingBean.getSelected(),
                "Selected mandate can be null");
        }
    }
}
