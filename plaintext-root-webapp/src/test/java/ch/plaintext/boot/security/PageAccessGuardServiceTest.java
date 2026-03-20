/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.security;

import ch.plaintext.MenuRegistry;
import ch.plaintext.boot.menu.MenuItemImpl;
import ch.plaintext.boot.menu.MenuRegistryImpl;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PageAccessGuardService - page access control based on menu visibility.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PageAccessGuardServiceTest {

    @Mock
    private MenuRegistryImpl menuRegistry;

    @Mock
    private FacesContext facesContext;

    @Mock
    private ExternalContext externalContext;

    private PageAccessGuardService service;

    @BeforeEach
    void setUp() {
        service = new PageAccessGuardService(menuRegistry);
    }

    // ==================== hasAccessToView Tests ====================

    @Test
    void hasAccessToView_shouldReturnTrue_whenViewIdNull() {
        assertTrue(service.hasAccessToView(null));
    }

    @Test
    void hasAccessToView_shouldReturnTrue_whenViewIdEmpty() {
        assertTrue(service.hasAccessToView(""));
    }

    @Test
    void hasAccessToView_shouldReturnTrue_forSystemPages() {
        assertTrue(service.hasAccessToView("/home.xhtml"));
        assertTrue(service.hasAccessToView("/index.xhtml"));
        assertTrue(service.hasAccessToView("/access-denied.xhtml"));
        assertTrue(service.hasAccessToView("/error.xhtml"));
        assertTrue(service.hasAccessToView("/login.xhtml"));
    }

    @Test
    void hasAccessToView_shouldReturnTrue_whenMenuIsVisible() {
        MenuItemImpl menuItem = mock(MenuItemImpl.class);
        when(menuItem.getCommand()).thenReturn("kontakte.html");
        when(menuItem.isOn()).thenReturn(true);
        when(menuItem.buildFullTitle()).thenReturn("Kontakte");

        when(menuRegistry.getAllMenuItemsImpl()).thenReturn(List.of(menuItem));

        assertTrue(service.hasAccessToView("/kontakte.xhtml"));
    }

    @Test
    void hasAccessToView_shouldReturnFalse_whenMenuIsNotVisible() {
        MenuItemImpl menuItem = mock(MenuItemImpl.class);
        when(menuItem.getCommand()).thenReturn("kontakte.html");
        when(menuItem.isOn()).thenReturn(false);
        when(menuItem.buildFullTitle()).thenReturn("Kontakte");

        when(menuRegistry.getAllMenuItemsImpl()).thenReturn(List.of(menuItem));

        assertFalse(service.hasAccessToView("/kontakte.xhtml"));
    }

    @Test
    void hasAccessToView_shouldReturnTrue_whenNoMenuMatchFound() {
        when(menuRegistry.getAllMenuItemsImpl()).thenReturn(new ArrayList<>());

        assertTrue(service.hasAccessToView("/unknown.xhtml"));
    }

    @Test
    void hasAccessToView_shouldReturnTrue_whenExceptionOccurs() {
        when(menuRegistry.getAllMenuItemsImpl()).thenThrow(new RuntimeException("error"));

        assertTrue(service.hasAccessToView("/kontakte.xhtml"));
    }

    @Test
    void hasAccessToView_shouldMatchCaseInsensitive() {
        MenuItemImpl menuItem = mock(MenuItemImpl.class);
        when(menuItem.getCommand()).thenReturn("Kontakte.html");
        when(menuItem.isOn()).thenReturn(true);
        when(menuItem.buildFullTitle()).thenReturn("Kontakte");

        when(menuRegistry.getAllMenuItemsImpl()).thenReturn(List.of(menuItem));

        assertTrue(service.hasAccessToView("/kontakte.xhtml"));
    }

    @Test
    void hasAccessToView_shouldConvertXhtmlToHtml() {
        MenuItemImpl menuItem = mock(MenuItemImpl.class);
        when(menuItem.getCommand()).thenReturn("kontakte.html");
        when(menuItem.isOn()).thenReturn(true);
        when(menuItem.buildFullTitle()).thenReturn("Kontakte");

        when(menuRegistry.getAllMenuItemsImpl()).thenReturn(List.of(menuItem));

        // Passes /kontakte.xhtml, should convert to kontakte.html to match
        assertTrue(service.hasAccessToView("/kontakte.xhtml"));
    }

    @Test
    void hasAccessToView_shouldHandleNullMenuCommand() {
        MenuItemImpl menuItem = mock(MenuItemImpl.class);
        when(menuItem.getCommand()).thenReturn(null);

        when(menuRegistry.getAllMenuItemsImpl()).thenReturn(List.of(menuItem));

        // No match found, should allow access
        assertTrue(service.hasAccessToView("/kontakte.xhtml"));
    }

    // ==================== redirectToAccessDenied Tests ====================

    @Test
    void redirectToAccessDenied_shouldRedirect() throws IOException {
        try (MockedStatic<FacesContext> mocked = mockStatic(FacesContext.class)) {
            mocked.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            when(facesContext.getExternalContext()).thenReturn(externalContext);
            when(externalContext.getRequestContextPath()).thenReturn("/app");

            service.redirectToAccessDenied();

            verify(externalContext).redirect("/app/access-denied.html");
            verify(facesContext).responseComplete();
        }
    }

    @Test
    void redirectToAccessDenied_shouldDoNothing_whenNoFacesContext() throws IOException {
        try (MockedStatic<FacesContext> mocked = mockStatic(FacesContext.class)) {
            mocked.when(FacesContext::getCurrentInstance).thenReturn(null);

            service.redirectToAccessDenied();

            // Should not throw
        }
    }
}
