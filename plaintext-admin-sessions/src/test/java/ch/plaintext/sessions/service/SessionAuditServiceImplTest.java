/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.sessions.service;

import ch.plaintext.sessions.entity.UserSession;
import ch.plaintext.sessions.repository.UserSessionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionAuditServiceImplTest {

    @Mock
    private UserSessionRepository repository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private SessionAuditServiceImpl service;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // entityManager is injected via @PersistenceContext (field injection)
    }

    @Test
    void updateOrCreateWithNullUserIdDoesNothing() {
        service.updateOrCreate(null, "sess-1", authentication, "ua");
        verifyNoInteractions(repository);
    }

    @Test
    void updateOrCreateWithNullSessionIdDoesNothing() {
        service.updateOrCreate(1L, null, authentication, "ua");
        verifyNoInteractions(repository);
    }

    @Test
    void updateOrCreateUpdatesExistingSession() {
        UserSession existing = new UserSession();
        existing.setId(1L);
        existing.setSessionId("sess-1");
        existing.setLastActivityTime(LocalDateTime.now().minusHours(1));

        when(repository.findBySessionId("sess-1")).thenReturn(List.of(existing));
        when(repository.save(any(UserSession.class))).thenReturn(existing);

        service.updateOrCreate(42L, "sess-1", authentication, "ua");

        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getLastActivityTime()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    void updateOrCreateCreatesNewSessionWhenNoneExists() {
        when(repository.findBySessionId("sess-new")).thenReturn(Collections.emptyList());
        when(authentication.getName()).thenReturn("testuser");

        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("PROPERTY_MANDAT_testMandat")
        );
        doReturn(authorities).when(authentication).getAuthorities();

        when(repository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateOrCreate(42L, "sess-new", authentication, "TestAgent/1.0");

        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(repository).save(captor.capture());
        UserSession saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getSessionId()).isEqualTo("sess-new");
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.getMandat()).isEqualTo("testMandat");
        assertThat(saved.getUserAgent()).isEqualTo("TestAgent/1.0");
        assertThat(saved.getActive()).isTrue();
    }

    @Test
    void updateOrCreateSetsUnknownMandatWhenNoMatchingAuthority() {
        when(repository.findBySessionId("sess-new")).thenReturn(Collections.emptyList());
        when(authentication.getName()).thenReturn("testuser");

        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        doReturn(authorities).when(authentication).getAuthorities();
        when(repository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateOrCreate(42L, "sess-new", authentication, "ua");

        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getMandat()).isEqualTo("unknown");
    }

    @Test
    void updateOrCreateSetsUnknownUsernameWhenAuthenticationIsNull() {
        when(repository.findBySessionId("sess-new")).thenReturn(Collections.emptyList());
        when(repository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateOrCreate(42L, "sess-new", null, "ua");

        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("unknown");
        assertThat(captor.getValue().getMandat()).isEqualTo("unknown");
    }

    @Test
    void updateOrCreateHandlesDuplicateSessions() {
        UserSession old = new UserSession();
        old.setId(1L);
        old.setSessionId("sess-dup");

        UserSession newer = new UserSession();
        newer.setId(2L);
        newer.setSessionId("sess-dup");

        List<UserSession> duplicates = new ArrayList<>(List.of(old, newer));
        when(repository.findBySessionId("sess-dup")).thenReturn(duplicates);
        when(repository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateOrCreate(42L, "sess-dup", authentication, "ua");

        // Should delete the older one (id=1) and keep the newer one (id=2)
        verify(repository).delete(old);
        verify(repository, never()).delete(newer);
        verify(repository).save(newer);
    }

    @Test
    void commitWithNullUserIdDoesNothing() {
        service.commit(null);
        verifyNoInteractions(repository);
    }

    @Test
    void commitDeactivatesAllActiveSessionsForUser() {
        UserSession s1 = new UserSession();
        s1.setId(1L);
        s1.setActive(true);

        UserSession s2 = new UserSession();
        s2.setId(2L);
        s2.setActive(true);

        when(repository.findByUserIdAndActive(42L, true)).thenReturn(List.of(s1, s2));
        when(repository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

        service.commit(42L);

        assertThat(s1.getActive()).isFalse();
        assertThat(s2.getActive()).isFalse();
        verify(repository, times(2)).save(any(UserSession.class));
    }

    @Test
    void getAllActiveSessionsReturnsActiveSessions() {
        UserSession s1 = new UserSession();
        s1.setActive(true);
        when(repository.findAllByActive(true)).thenReturn(List.of(s1));

        List<UserSession> result = service.getAllActiveSessions();
        assertThat(result).hasSize(1);
    }

    @Test
    void getAllActiveSessionsReturnsEmptyListOnException() {
        when(repository.findAllByActive(true)).thenThrow(new RuntimeException("DB error"));

        List<UserSession> result = service.getAllActiveSessions();
        assertThat(result).isEmpty();
    }

    @Test
    void getActiveSessionsByMandatReturnsSessionsForMandat() {
        UserSession s1 = new UserSession();
        when(repository.findByMandatAndActive("testMandat", true)).thenReturn(List.of(s1));

        List<UserSession> result = service.getActiveSessionsByMandat("testMandat");
        assertThat(result).hasSize(1);
    }

    @Test
    void getActiveSessionsByMandatReturnsEmptyForNullMandat() {
        List<UserSession> result = service.getActiveSessionsByMandat(null);
        assertThat(result).isEmpty();
    }

    @Test
    void getActiveSessionsByMandatReturnsEmptyForEmptyMandat() {
        List<UserSession> result = service.getActiveSessionsByMandat("  ");
        assertThat(result).isEmpty();
    }

    @Test
    void forceLogoutDeactivatesSession() {
        UserSession session = new UserSession();
        session.setActive(true);
        session.setUsername("admin");

        when(repository.findBySessionId("sess-1")).thenReturn(List.of(session));
        when(repository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

        service.forceLogout("sess-1");

        assertThat(session.getActive()).isFalse();
        verify(repository).save(session);
    }

    @Test
    void forceLogoutWithNullSessionIdDoesNothing() {
        service.forceLogout(null);
        verifyNoInteractions(repository);
    }

    @Test
    void forceLogoutWithEmptySessionIdDoesNothing() {
        service.forceLogout("  ");
        verifyNoInteractions(repository);
    }

    @Test
    void forceLogoutWhenSessionNotFoundDoesNotThrow() {
        when(repository.findBySessionId("nonexistent")).thenReturn(Collections.emptyList());

        service.forceLogout("nonexistent");

        verify(repository, never()).save(any());
    }
}
