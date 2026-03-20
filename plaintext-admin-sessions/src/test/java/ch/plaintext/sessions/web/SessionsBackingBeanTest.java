/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.sessions.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.sessions.entity.UserSession;
import ch.plaintext.sessions.service.SessionAuditServiceImpl;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionsBackingBeanTest {

    @Mock
    private SessionAuditServiceImpl sessionService;

    @Mock
    private PlaintextSecurity security;

    @Mock
    private FacesContext facesContext;

    @Mock
    private ExternalContext externalContext;

    private SessionsBackingBean bean;

    private MockedStatic<FacesContext> facesContextMock;

    @BeforeEach
    void setUp() {
        bean = new SessionsBackingBean(sessionService, security);
        facesContextMock = mockStatic(FacesContext.class);
        facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
    }

    @AfterEach
    void tearDown() {
        facesContextMock.close();
    }

    @Test
    void initAsRootLoadsAllActiveSessions() {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(true);
        UserSession s1 = new UserSession();
        when(sessionService.getAllActiveSessions()).thenReturn(List.of(s1));

        bean.init();

        assertThat(bean.isRoot()).isTrue();
        assertThat(bean.getSessions()).hasSize(1);
        verify(sessionService).getAllActiveSessions();
    }

    @Test
    void initAsAdminLoadsSessionsByMandat() {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(false);
        when(security.getMandat()).thenReturn("testMandat");
        UserSession s1 = new UserSession();
        when(sessionService.getActiveSessionsByMandat("testMandat")).thenReturn(List.of(s1));

        bean.init();

        assertThat(bean.isRoot()).isFalse();
        assertThat(bean.getSessions()).hasSize(1);
        verify(sessionService).getActiveSessionsByMandat("testMandat");
    }

    @Test
    void initHandlesException() {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(true);
        when(sessionService.getAllActiveSessions()).thenThrow(new RuntimeException("DB error"));

        bean.init();

        verify(facesContext).addMessage(isNull(), any(FacesMessage.class));
    }

    @Test
    void selectDoesNothing() {
        // Just verify it doesn't throw
        bean.select();
    }

    @Test
    void clearSelectionSetsSelectedToNull() {
        UserSession session = new UserSession();
        bean.setSelected(session);

        bean.clearSelection();

        assertThat(bean.getSelected()).isNull();
    }

    @Test
    void refreshReloadsDataAndClearsSelection() {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(true);
        UserSession s1 = new UserSession();
        when(sessionService.getAllActiveSessions()).thenReturn(List.of(s1));
        bean.init();

        bean.setSelected(s1);
        bean.refresh();

        assertThat(bean.getSelected()).isNull();
        verify(facesContext, atLeastOnce()).addMessage(isNull(), any(FacesMessage.class));
    }

    @Test
    void forceLogoutWithNoSelectionShowsWarning() {
        bean.setSelected(null);

        bean.forceLogout();

        verify(facesContext).addMessage(isNull(), argThat(msg ->
                msg.getSeverity() == FacesMessage.SEVERITY_WARN));
        verifyNoInteractions(sessionService);
    }

    @Test
    void forceLogoutWithSelectionCallsService() {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(true);
        when(sessionService.getAllActiveSessions()).thenReturn(List.of());
        bean.init();

        UserSession session = new UserSession();
        session.setSessionId("sess-123");
        bean.setSelected(session);

        bean.forceLogout();

        verify(sessionService).forceLogout("sess-123");
        assertThat(bean.getSelected()).isNull();
    }

    @Test
    void forceLogoutHandlesServiceException() {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(true);
        when(sessionService.getAllActiveSessions()).thenReturn(List.of());
        bean.init();

        UserSession session = new UserSession();
        session.setSessionId("sess-err");
        bean.setSelected(session);

        doThrow(new RuntimeException("Force logout error")).when(sessionService).forceLogout("sess-err");

        bean.forceLogout();

        verify(facesContext, atLeastOnce()).addMessage(isNull(), argThat(msg ->
                msg.getSeverity() == FacesMessage.SEVERITY_ERROR));
    }

    @Test
    void checkAccessRedirectsWhenNoAdminOrRoot() throws Exception {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(false);
        when(sessionService.getActiveSessionsByMandat(any())).thenReturn(List.of());
        bean.init();

        when(security.ifGranted("ROLE_ADMIN")).thenReturn(false);
        when(facesContext.getExternalContext()).thenReturn(externalContext);

        bean.checkAccess();

        verify(externalContext).redirect("/index.xhtml");
    }

    @Test
    void checkAccessDoesNotRedirectForAdmin() throws Exception {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(false);
        when(security.ifGranted("ROLE_ADMIN")).thenReturn(true);
        when(security.getMandat()).thenReturn("m1");
        when(sessionService.getActiveSessionsByMandat("m1")).thenReturn(List.of());
        bean.init();

        bean.checkAccess();

        verify(externalContext, never()).redirect(any());
    }

    @Test
    void checkAccessDoesNotRedirectForRoot() throws Exception {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(true);
        when(sessionService.getAllActiveSessions()).thenReturn(List.of());
        bean.init();

        bean.checkAccess();

        verify(externalContext, never()).redirect(any());
    }

    @Test
    void checkAccessHandlesRedirectException() throws Exception {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(false);
        when(security.ifGranted("ROLE_ADMIN")).thenReturn(false);
        when(security.getMandat()).thenReturn("m1");
        when(sessionService.getActiveSessionsByMandat("m1")).thenReturn(List.of());
        bean.init();

        when(facesContext.getExternalContext()).thenReturn(externalContext);
        doThrow(new java.io.IOException("Redirect failed")).when(externalContext).redirect("/index.xhtml");

        // Should not throw
        bean.checkAccess();
    }
}
