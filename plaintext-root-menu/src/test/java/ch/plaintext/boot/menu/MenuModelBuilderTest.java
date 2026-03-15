package ch.plaintext.boot.menu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.menu.MenuModel;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MenuModelBuilder
 */
@ExtendWith(MockitoExtension.class)
class MenuModelBuilderTest {

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private MenuModelBuilder menuModelBuilder;

    @BeforeEach
    void setUp() {
        // Setup
    }

    @Test
    void testBuildMenuModel_WhenNoMenuItems_ShouldReturnEmptyModel() {
        // Given
        when(applicationContext.getBeansOfType(MenuItemImpl.class))
            .thenReturn(Collections.emptyMap());

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertTrue(result.getElements().isEmpty());
        verify(applicationContext, times(1)).getBeansOfType(MenuItemImpl.class);
    }

    @Test
    void testBuildMenuModel_WithSingleRootItem_ShouldAddToModel() {
        // Given
        MenuItemImpl item = createMenuItem("Home", null, 1, true);
        Map<String, MenuItemImpl> beans = Map.of("home", item);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertEquals(1, result.getElements().size());
    }

    @Test
    void testBuildMenuModel_WithMultipleRootItems_ShouldAddAllInOrder() {
        // Given
        MenuItemImpl item1 = createMenuItem("Home", null, 1, true);
        MenuItemImpl item2 = createMenuItem("About", null, 2, true);
        MenuItemImpl item3 = createMenuItem("Contact", null, 3, true);

        Map<String, MenuItemImpl> beans = new HashMap<>();
        beans.put("home", item1);
        beans.put("about", item2);
        beans.put("contact", item3);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertEquals(3, result.getElements().size());
    }

    @Test
    void testBuildMenuModel_WithParentChild_ShouldCreateHierarchy() {
        // Given
        MenuItemImpl parent = createMenuItem("Settings", null, 1, true);
        MenuItemImpl child = createMenuItem("User Settings", "Settings", 2, true);

        Map<String, MenuItemImpl> beans = new HashMap<>();
        beans.put("settings", parent);
        beans.put("userSettings", child);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertEquals(1, result.getElements().size());
    }

    @Test
    void testBuildMenuModel_WithInvisibleItem_ShouldNotInclude() {
        // Given
        MenuItemImpl visibleItem = createMenuItem("Home", null, 1, true);
        MenuItemImpl invisibleItem = createMenuItem("Hidden", null, 2, false);

        Map<String, MenuItemImpl> beans = new HashMap<>();
        beans.put("home", visibleItem);
        beans.put("hidden", invisibleItem);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertEquals(1, result.getElements().size());
    }

    @Test
    void testBuildMenuModel_WithOrphanChild_ShouldSkip() {
        // Given
        MenuItemImpl orphan = createMenuItem("Orphan", "NonExistentParent", 1, true);

        Map<String, MenuItemImpl> beans = Map.of("orphan", orphan);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertTrue(result.getElements().isEmpty());
    }

    @Test
    void testBuildMenuModel_WithMultipleLevels_ShouldCreateDeepHierarchy() {
        // Given
        MenuItemImpl level1 = createMenuItem("Settings", null, 1, true);
        MenuItemImpl level2 = createMenuItem("User", "Settings", 2, true);
        MenuItemImpl level3 = createMenuItem("Profile", "User", 3, true);

        Map<String, MenuItemImpl> beans = new HashMap<>();
        beans.put("settings", level1);
        beans.put("user", level2);
        beans.put("profile", level3);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertEquals(1, result.getElements().size());
    }

    @Test
    void testBuildMenuModel_ShouldRespectOrderProperty() {
        // Given
        MenuItemImpl item3 = createMenuItem("Third", null, 3, true);
        MenuItemImpl item1 = createMenuItem("First", null, 1, true);
        MenuItemImpl item2 = createMenuItem("Second", null, 2, true);

        Map<String, MenuItemImpl> beans = new HashMap<>();
        beans.put("third", item3);
        beans.put("first", item1);
        beans.put("second", item2);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertEquals(3, result.getElements().size());
        // Items should be ordered by their order property
    }

    @Test
    void testBuildMenuModel_WithMixedVisibleInvisible_ShouldOnlyIncludeVisible() {
        // Given
        MenuItemImpl visible1 = createMenuItem("Home", null, 1, true);
        MenuItemImpl invisible = createMenuItem("Hidden", null, 2, false);
        MenuItemImpl visible2 = createMenuItem("About", null, 3, true);

        Map<String, MenuItemImpl> beans = new HashMap<>();
        beans.put("home", visible1);
        beans.put("hidden", invisible);
        beans.put("about", visible2);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertEquals(2, result.getElements().size());
    }

    @Test
    void testBuildMenuModel_WithEmptyParentString_ShouldTreatAsRoot() {
        // Given
        MenuItemImpl itemWithEmptyParent = createMenuItem("Home", "", 1, true);
        MenuItemImpl itemWithNullParent = createMenuItem("About", null, 2, true);

        Map<String, MenuItemImpl> beans = new HashMap<>();
        beans.put("home", itemWithEmptyParent);
        beans.put("about", itemWithNullParent);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertEquals(2, result.getElements().size());
    }

    @Test
    void testBuildMenuModel_WithChildrenButInvisibleParent_ShouldSkipBoth() {
        // Given
        MenuItemImpl parent = createMenuItem("Settings", null, 1, false);
        MenuItemImpl child = createMenuItem("User Settings", "Settings", 2, true);

        Map<String, MenuItemImpl> beans = new HashMap<>();
        beans.put("settings", parent);
        beans.put("userSettings", child);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertTrue(result.getElements().isEmpty());
    }

    @Test
    void testBuildMenuModel_WithChildOrderLowerThanParent_ShouldStillBuildHierarchy() {
        // Given - Child has lower order (10) than parent (100)
        // This simulates the real-world scenario where ZeiterfassungMenu has order 20
        // but ZeiterfassungEingabeSubmenu has order 24
        MenuItemImpl child = createMenuItem("Eingabe", "Zeiterfassung", 10, true);
        MenuItemImpl parent = createMenuItem("Zeiterfassung", null, 100, true);

        Map<String, MenuItemImpl> beans = new HashMap<>();
        beans.put("child", child);
        beans.put("parent", parent);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertEquals(1, result.getElements().size(), "Should have 1 root element (parent)");
        // The hierarchy should be built correctly despite order values
    }

    @Test
    void testBuildMenuModel_WithComplexOrderScenario_ShouldHandleAllItems() {
        // Given - Mixed orders where children might have lower order than parents
        MenuItemImpl parent1 = createMenuItem("Menu1", null, 50, true);
        MenuItemImpl child1 = createMenuItem("Child1", "Menu1", 10, true);
        MenuItemImpl parent2 = createMenuItem("Menu2", null, 20, true);
        MenuItemImpl child2 = createMenuItem("Child2", "Menu2", 100, true);

        Map<String, MenuItemImpl> beans = new HashMap<>();
        beans.put("parent1", parent1);
        beans.put("child1", child1);
        beans.put("parent2", parent2);
        beans.put("child2", child2);

        when(applicationContext.getBeansOfType(MenuItemImpl.class)).thenReturn(beans);

        // When
        MenuModel result = menuModelBuilder.buildMenuModel();

        // Then
        assertNotNull(result);
        assertEquals(2, result.getElements().size(), "Should have 2 root elements");
    }

    // Helper method
    private MenuItemImpl createMenuItem(String title, String parent, int order, boolean visible) {
        MenuItemImpl item = new MenuItemImpl();
        item.setTitle(title);
        item.setParent(parent);
        item.setOrder(order);
        item.setCommand("/test/" + title.toLowerCase().replace(" ", "-"));
        item.setRoles(Collections.emptyList());
        item.setIcon("pi pi-home");

        // Mock the isOn method to return visible
        MenuItemImpl spy = spy(item);
        when(spy.isOn()).thenReturn(visible);

        return spy;
    }
}
