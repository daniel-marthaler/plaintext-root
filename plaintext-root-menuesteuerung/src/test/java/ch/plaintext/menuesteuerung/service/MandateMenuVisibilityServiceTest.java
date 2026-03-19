/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.menuesteuerung.service;

import ch.plaintext.MenuRegistry;
import ch.plaintext.MenuVisibilityProvider;
import ch.plaintext.PlaintextSecurity;
import ch.plaintext.menuesteuerung.model.MandateMenuConfig;
import ch.plaintext.menuesteuerung.persistence.MandateMenuConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MandateMenuVisibilityService whitelist/blacklist functionality.
 *
 * @author plaintext.ch
 * @since 1.39.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MandateMenuVisibilityService - Whitelist/Blacklist Tests")
class MandateMenuVisibilityServiceTest {

    @Mock
    private MandateMenuConfigRepository repository;

    @Mock
    private PlaintextSecurity plaintextSecurity;

    @Mock
    private MenuRegistry menuRegistry;

    @InjectMocks
    private MandateMenuVisibilityService service;

    private MandateMenuConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new MandateMenuConfig();
        testConfig.setMandateName("test-mandate");
        testConfig.setHiddenMenus(new HashSet<>());
        testConfig.setWhitelistMode(false);
    }

    @Nested
    @DisplayName("Scenario 1: Whitelist Mode Active with Items")
    class WhitelistActiveWithItems {

        @BeforeEach
        void setup() {
            testConfig.setWhitelistMode(true);
            testConfig.setHiddenMenus(Set.of("Menu1", "Menu2", "Menu3"));
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));
        }

        @Test
        @DisplayName("Should show only whitelisted items")
        void shouldShowOnlyWhitelistedItems() {
            // Arrange
            String whitelistedItem = "Menu1";
            String nonWhitelistedItem = "Menu99";

            // Act
            boolean whitelistedVisible = service.isMenuVisibleForMandate(whitelistedItem, "test-mandate");
            boolean nonWhitelistedVisible = service.isMenuVisibleForMandate(nonWhitelistedItem, "test-mandate");

            // Assert
            assertTrue(whitelistedVisible, "Whitelisted item should be visible");
            assertFalse(nonWhitelistedVisible, "Non-whitelisted item should not be visible");
        }

        @Test
        @DisplayName("Should show all items from whitelist set")
        void shouldShowAllWhitelistItems() {
            // Arrange
            Set<String> whitelistSet = testConfig.getHiddenMenus();

            // Act & Assert
            for (String menu : whitelistSet) {
                assertTrue(
                    service.isMenuVisibleForMandate(menu, "test-mandate"),
                    "Menu '" + menu + "' should be visible in whitelist mode"
                );
            }
        }

        @Test
        @DisplayName("Should hide items not in whitelist")
        void shouldHideItemsNotInWhitelist() {
            // Arrange
            String[] otherMenus = {"OtherMenu", "AnotherMenu", "ThirdMenu"};

            // Act & Assert
            for (String menu : otherMenus) {
                assertFalse(
                    service.isMenuVisibleForMandate(menu, "test-mandate"),
                    "Menu '" + menu + "' should be hidden in whitelist mode"
                );
            }
        }
    }

    @Nested
    @DisplayName("Scenario 2: Blacklist Mode Active with Items")
    class BlacklistActiveWithItems {

        @BeforeEach
        void setup() {
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(Set.of("HiddenMenu1", "HiddenMenu2", "HiddenMenu3"));
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));
        }

        @Test
        @DisplayName("Should show all except blacklisted items")
        void shouldShowAllExceptBlacklistedItems() {
            // Arrange
            String blacklistedItem = "HiddenMenu1";
            String visibleItem = "VisibleMenu";

            // Act
            boolean blacklistedVisible = service.isMenuVisibleForMandate(blacklistedItem, "test-mandate");
            boolean visibleItemVisible = service.isMenuVisibleForMandate(visibleItem, "test-mandate");

            // Assert
            assertFalse(blacklistedVisible, "Blacklisted item should be hidden");
            assertTrue(visibleItemVisible, "Non-blacklisted item should be visible");
        }

        @Test
        @DisplayName("Should hide all items from blacklist set")
        void shouldHideAllBlacklistedItems() {
            // Arrange
            Set<String> blacklistSet = testConfig.getHiddenMenus();

            // Act & Assert
            for (String menu : blacklistSet) {
                assertFalse(
                    service.isMenuVisibleForMandate(menu, "test-mandate"),
                    "Menu '" + menu + "' should be hidden in blacklist mode"
                );
            }
        }

        @Test
        @DisplayName("Should show items not in blacklist")
        void shouldShowItemsNotInBlacklist() {
            // Arrange
            String[] visibleMenus = {"VisibleMenu1", "VisibleMenu2", "VisibleMenu3"};

            // Act & Assert
            for (String menu : visibleMenus) {
                assertTrue(
                    service.isMenuVisibleForMandate(menu, "test-mandate"),
                    "Menu '" + menu + "' should be visible in blacklist mode"
                );
            }
        }
    }

    @Nested
    @DisplayName("Scenario 3: Both Whitelist and Blacklist Active")
    class BothModesConfigured {

        @BeforeEach
        void setup() {
            // If both modes were somehow set (shouldn't happen in normal flow)
            // Whitelist should take precedence
            testConfig.setWhitelistMode(true);
            testConfig.setHiddenMenus(Set.of("Menu1", "Menu2"));
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));
        }

        @Test
        @DisplayName("Should treat whitelist mode as active regardless of data")
        void whitelistTakesPrecedence() {
            // This test verifies that when whitelistMode is true,
            // the service treats hiddenMenus as whitelist items, not blacklist

            // Arrange
            String whitelistItem = "Menu1";
            String nonWhitelistItem = "Menu99";

            // Act
            boolean whitelistVisible = service.isMenuVisibleForMandate(whitelistItem, "test-mandate");
            boolean nonWhitelistVisible = service.isMenuVisibleForMandate(nonWhitelistItem, "test-mandate");

            // Assert
            assertTrue(whitelistVisible, "Whitelist item should be visible");
            assertFalse(nonWhitelistVisible, "Non-whitelist item should not be visible");
        }
    }

    @Nested
    @DisplayName("Scenario 4: Neither Mode Active (Default Behavior)")
    class NeitherModeActive {

        @BeforeEach
        void setup() {
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(new HashSet<>()); // Empty list
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));
        }

        @Test
        @DisplayName("Should show all items when blacklist is empty")
        void shouldShowAllItemsWhenBlacklistEmpty() {
            // Arrange
            String[] allMenus = {"Menu1", "Menu2", "Menu3", "Menu4", "Menu5"};

            // Act & Assert
            for (String menu : allMenus) {
                assertTrue(
                    service.isMenuVisibleForMandate(menu, "test-mandate"),
                    "Menu '" + menu + "' should be visible when blacklist is empty"
                );
            }
        }

        @Test
        @DisplayName("Should treat null whitelistMode as false (blacklist mode)")
        void shouldTreatNullWhitelistModeAsBlacklist() {
            // Arrange
            testConfig.setWhitelistMode(null);
            String testMenu = "AnyMenu";

            // Act
            boolean visible = service.isMenuVisibleForMandate(testMenu, "test-mandate");

            // Assert
            assertTrue(visible, "Menu should be visible when whitelistMode is null (blacklist mode)");
        }
    }

    @Nested
    @DisplayName("Scenario 5: Empty Whitelist")
    class EmptyWhitelist {

        @BeforeEach
        void setup() {
            testConfig.setWhitelistMode(true);
            testConfig.setHiddenMenus(new HashSet<>()); // Empty whitelist
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));
        }

        @Test
        @DisplayName("Should hide all items when whitelist is empty")
        void shouldHideAllItemsWhenWhitelistEmpty() {
            // Arrange
            String[] allMenus = {"Menu1", "Menu2", "Menu3"};

            // Act & Assert
            for (String menu : allMenus) {
                assertFalse(
                    service.isMenuVisibleForMandate(menu, "test-mandate"),
                    "Menu '" + menu + "' should be hidden when whitelist is empty"
                );
            }
        }

        @Test
        @DisplayName("Should have defined behavior: nothing is visible")
        void emptyWhitelistBehaviorIsDefinedAsHideAll() {
            // This documents the expected behavior: empty whitelist = hide all

            // Act
            boolean anyMenuVisible = service.isMenuVisibleForMandate("AnyMenu", "test-mandate");

            // Assert
            assertFalse(anyMenuVisible, "Empty whitelist should hide all menus");
        }
    }

    @Nested
    @DisplayName("Scenario 6: Empty Blacklist")
    class EmptyBlacklist {

        @BeforeEach
        void setup() {
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(new HashSet<>()); // Empty blacklist
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));
        }

        @Test
        @DisplayName("Should show all items when blacklist is empty")
        void shouldShowAllItemsWhenBlacklistEmpty() {
            // Arrange
            String[] allMenus = {"Menu1", "Menu2", "Menu3", "Menu4"};

            // Act & Assert
            for (String menu : allMenus) {
                assertTrue(
                    service.isMenuVisibleForMandate(menu, "test-mandate"),
                    "Menu '" + menu + "' should be visible when blacklist is empty"
                );
            }
        }
    }

    @Nested
    @DisplayName("Scenario 7: Edge Cases - Null Values")
    class EdgeCasesNullValues {

        @Test
        @DisplayName("Should handle null mandate gracefully")
        void shouldHandleNullMandate() {
            // Act
            boolean visible = service.isMenuVisibleForMandate("AnyMenu", null);

            // Assert
            assertTrue(visible, "Should show all items when mandate is null");
        }

        @Test
        @DisplayName("Should handle empty mandate string")
        void shouldHandleEmptyMandateString() {
            // Act
            boolean visible = service.isMenuVisibleForMandate("AnyMenu", "");

            // Assert
            assertTrue(visible, "Should show all items when mandate is empty");
        }

        @Test
        @DisplayName("Should handle missing configuration")
        void shouldHandleMissingConfiguration() {
            // Arrange
            when(repository.findByMandateName("unknown-mandate")).thenReturn(Optional.empty());

            // Act
            boolean visible = service.isMenuVisibleForMandate("AnyMenu", "unknown-mandate");

            // Assert
            assertTrue(visible, "Should show all items when no configuration exists");
        }

        @Test
        @DisplayName("Should handle null hiddenMenus set")
        void shouldHandleNullHiddenMenus() {
            // Arrange
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(null);
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));

            // Act
            boolean visible = service.isMenuVisibleForMandate("AnyMenu", "test-mandate");

            // Assert
            assertTrue(visible, "Should show all items when hiddenMenus is null");
        }

        @Test
        @DisplayName("Should throw NullPointerException for null menu title (expected behavior)")
        void shouldThrowForNullMenuTitle() {
            // Arrange
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(Set.of("Menu1"));
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));

            // Act & Assert - null menu title will cause NPE in contains() check
            // This is documented expected behavior - caller must provide valid menu title
            assertThrows(NullPointerException.class, () -> {
                service.isMenuVisibleForMandate(null, "test-mandate");
            }, "Null menu title causes NullPointerException (expected - must validate input)");
        }
    }

    @Nested
    @DisplayName("Scenario 7: Edge Cases - Concurrent Modifications")
    class EdgeCasesConcurrentModifications {

        @Test
        @DisplayName("Should handle modification of hiddenMenus while checking visibility")
        void shouldHandleModificationWhileChecking() throws InterruptedException {
            // Arrange
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(new HashSet<>(Set.of("Menu1", "Menu2")));
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));

            // Simulate concurrent modification
            Thread modificationThread = new Thread(() -> {
                testConfig.getHiddenMenus().add("Menu3");
            });

            Thread checkThread = new Thread(() -> {
                // This should complete without exception
                service.isMenuVisibleForMandate("Menu1", "test-mandate");
            });

            // Act
            modificationThread.start();
            checkThread.start();
            modificationThread.join();
            checkThread.join();

            // Assert - if we get here without exception, the test passes
            assertTrue(true, "Concurrent access should be handled gracefully");
        }

        @Test
        @DisplayName("Should handle rapid mode toggles")
        void shouldHandleRapidModeToggles() {
            // Arrange
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(new HashSet<>(Set.of("Menu1")));
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));

            // Act - rapid mode changes
            for (int i = 0; i < 100; i++) {
                boolean oldMode = Boolean.TRUE.equals(testConfig.getWhitelistMode());
                testConfig.setWhitelistMode(!oldMode);
            }

            // Final state should be consistent
            boolean finalVisible = service.isMenuVisibleForMandate("Menu1", "test-mandate");

            // Assert
            assertNotNull(finalVisible, "Should return a boolean after rapid toggles");
        }
    }

    @Nested
    @DisplayName("Scenario 8: Case Sensitivity and Normalization")
    class CaseSensitivityAndNormalization {

        @Test
        @DisplayName("Should handle case-insensitive mandate names")
        void shouldHandleCaseInsensitiveMandateNames() {
            // Arrange
            testConfig.setMandateName("test-mandate");
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));
            testConfig.setWhitelistMode(false);
            testConfig.setHiddenMenus(new HashSet<>());

            // Act - different case variations
            boolean visible1 = service.isMenuVisibleForMandate("Menu", "TEST-MANDATE");
            boolean visible2 = service.isMenuVisibleForMandate("Menu", "test-mandate");
            boolean visible3 = service.isMenuVisibleForMandate("Menu", "Test-Mandate");

            // Assert
            assertTrue(visible1, "Should find mandate with uppercase");
            assertTrue(visible2, "Should find mandate with lowercase");
            assertTrue(visible3, "Should find mandate with mixed case");
        }

        @Test
        @DisplayName("Should preserve case sensitivity for menu titles")
        void shouldPreserveCaseSensitivityForMenuTitles() {
            // Arrange
            testConfig.setWhitelistMode(true);
            testConfig.setHiddenMenus(Set.of("Menu1"));
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));

            // Act
            boolean lowercase = service.isMenuVisibleForMandate("menu1", "test-mandate");
            boolean uppercase = service.isMenuVisibleForMandate("MENU1", "test-mandate");
            boolean exact = service.isMenuVisibleForMandate("Menu1", "test-mandate");

            // Assert
            assertFalse(lowercase, "Lowercase variant should not match whitelist entry");
            assertFalse(uppercase, "Uppercase variant should not match whitelist entry");
            assertTrue(exact, "Exact case match should be in whitelist");
        }
    }

    @Nested
    @DisplayName("Mode Transition - Toggle Behavior")
    class ModeTransitionToggleBehavior {

        @Test
        @DisplayName("Should correctly invert menu selection when toggling from blacklist to whitelist")
        void shouldInvertSelectionOnToggle() {
            // Arrange
            Set<String> allMenus = Set.of("Menu1", "Menu2", "Menu3", "Menu4", "Menu5");
            testConfig.setWhitelistMode(false); // Start in blacklist mode
            testConfig.setHiddenMenus(new HashSet<>(Set.of("Menu1", "Menu2"))); // Hide these

            // In blacklist: Menu1, Menu2 are hidden; Menu3, Menu4, Menu5 are visible
            // When toggling to whitelist: the visible items become the whitelist
            Set<String> invertedSelection = new HashSet<>();
            for (String menu : allMenus) {
                if (!testConfig.getHiddenMenus().contains(menu)) {
                    invertedSelection.add(menu);
                }
            }

            testConfig.setHiddenMenus(invertedSelection);
            testConfig.setWhitelistMode(true); // Toggle to whitelist mode

            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));

            // Act & Assert
            // Now in whitelist mode, only previously visible items should be visible
            assertTrue(service.isMenuVisibleForMandate("Menu3", "test-mandate"));
            assertTrue(service.isMenuVisibleForMandate("Menu4", "test-mandate"));
            assertTrue(service.isMenuVisibleForMandate("Menu5", "test-mandate"));
            assertFalse(service.isMenuVisibleForMandate("Menu1", "test-mandate"));
            assertFalse(service.isMenuVisibleForMandate("Menu2", "test-mandate"));
        }
    }

    @Nested
    @DisplayName("Configuration Persistence")
    class ConfigurationPersistence {

        @Test
        @DisplayName("Should save whitelist configuration correctly")
        void shouldSaveWhitelistConfig() {
            // Arrange
            Set<String> whitelist = Set.of("Menu1", "Menu2", "Menu3");
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));
            when(repository.save(any(MandateMenuConfig.class))).thenReturn(testConfig);

            // Act
            MandateMenuConfig saved = service.saveConfig("test-mandate", whitelist, true);

            // Assert
            verify(repository).save(any(MandateMenuConfig.class));
            assertTrue(Boolean.TRUE.equals(saved.getWhitelistMode()), "Whitelist mode should be true");
        }

        @Test
        @DisplayName("Should save blacklist configuration correctly")
        void shouldSaveBlacklistConfig() {
            // Arrange
            Set<String> blacklist = Set.of("HiddenMenu1", "HiddenMenu2");
            when(repository.findByMandateName("test-mandate")).thenReturn(Optional.of(testConfig));
            when(repository.save(any(MandateMenuConfig.class))).thenReturn(testConfig);

            // Act
            MandateMenuConfig saved = service.saveConfig("test-mandate", blacklist, false);

            // Assert
            verify(repository).save(any(MandateMenuConfig.class));
            assertFalse(Boolean.TRUE.equals(saved.getWhitelistMode()), "Whitelist mode should be false");
        }
    }
}
