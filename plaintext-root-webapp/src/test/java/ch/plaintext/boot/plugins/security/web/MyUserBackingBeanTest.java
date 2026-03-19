/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.boot.plugins.security.model.MyRememberMe;
import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import ch.plaintext.boot.plugins.security.persistence.MyRememberMeRepository;
import ch.plaintext.boot.plugins.security.persistence.MyUserRepository;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MyUserBackingBean
 */
@ExtendWith(MockitoExtension.class)
class MyUserBackingBeanTest {

    @Mock
    private MyUserRepository repo;

    @Mock
    private MyRememberMeRepository rememberMeRepo;

    @Mock
    private PlaintextSecurity plaintextSecurity;

    @Mock
    private FacesContext facesContext;

    @InjectMocks
    private MyUserBackingBean backingBean;

    private MyUserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new MyUserEntity();
        testUser.setId(1L);
        testUser.setUsername("test@example.com");
        testUser.setPassword("$2a$10$hashedPassword");
        testUser.setMandat("test_mandat");

        lenient().when(plaintextSecurity.getMandat()).thenReturn("test_mandat");
        // Mock admin role so init() will call repo.findAll()
        lenient().when(plaintextSecurity.ifGranted("ROLE_admin")).thenReturn(true);
        lenient().when(plaintextSecurity.ifGranted("ROLE_root")).thenReturn(false);
    }

    @Test
    void testInit_ShouldLoadUsersAndRememberMes() {
        // Given
        List<MyUserEntity> usersList = new ArrayList<>();
        usersList.add(testUser);
        List<MyRememberMe> rememberMes = new ArrayList<>();

        when(repo.findAll()).thenReturn(usersList);
        when(rememberMeRepo.findAll()).thenReturn(rememberMes);

        // When
        backingBean.init();

        // Then
        assertNotNull(backingBean.getUsers());
        assertEquals(1, backingBean.getUsers().size(),
            "Expected 1 user to be loaded for mandate test_mandat");
        assertEquals(testUser, backingBean.getUsers().get(0));
        verify(repo, times(1)).findAll();
        verify(rememberMeRepo, times(1)).findAll();
    }

    @Test
    void testNewUser_ShouldCreateAndSaveUser() {
        // Given
        MyUserEntity newUser = new MyUserEntity();
        newUser.setId(2L);

        when(repo.save(any(MyUserEntity.class))).thenReturn(newUser);
        when(repo.findAll()).thenReturn(Arrays.asList(newUser));
        when(rememberMeRepo.findAll()).thenReturn(new ArrayList<>());

        // When
        backingBean.newUser();

        // Then
        assertEquals(newUser, backingBean.getSelected());
        verify(repo, times(1)).save(any(MyUserEntity.class));
        verify(repo, times(2)).findAll(); // Called by both init() and extractRolesFromDatabase()
    }

    @Test
    void testSelect_ShouldSetPasswordField() {
        // Given
        backingBean.setSelected(testUser);

        // When
        backingBean.select();

        // Then
        assertEquals(testUser.getPassword(), backingBean.getMyUserPw());
    }

    //@Test
    void testDelete_ShouldDeleteUserAndReload() {
        // Given
        backingBean.setSelected(testUser);
        when(repo.findAll()).thenReturn(new ArrayList<>());
        when(rememberMeRepo.findAll()).thenReturn(new ArrayList<>());

        // When
        try (MockedStatic<FacesContext> facesContextMock = mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            backingBean.delete();
        }

        // Then
        assertNull(backingBean.getSelected());
        verify(repo, times(1)).delete(testUser);
        verify(repo, times(1)).findAll();
        verify(facesContext, times(1)).addMessage(isNull(), any(FacesMessage.class));
    }

    //@Test
    void testDeleteRememberMe_ShouldDeleteAndReload() {
        // Given
        MyRememberMe rememberMe = new MyRememberMe();
        backingBean.setSelectedRememberMe(rememberMe);
        when(repo.findAll()).thenReturn(new ArrayList<>());
        when(rememberMeRepo.findAll()).thenReturn(new ArrayList<>());

        // When
        backingBean.deleteRememberMe();

        // Then
        verify(rememberMeRepo, times(1)).delete(rememberMe);
        verify(repo, times(1)).findAll();
        verify(rememberMeRepo, times(1)).findAll(); // once in init (called by deleteRememberMe)
    }

    @Test
    void testOnToggle_ShouldToggleRemlistColapsed() {
        // Given
        boolean initialState = backingBean.isRemlistcolapsed();

        // When
        backingBean.onToggle();

        // Then
        assertEquals(!initialState, backingBean.isRemlistcolapsed());

        // Toggle again
        backingBean.onToggle();
        assertEquals(initialState, backingBean.isRemlistcolapsed());
    }

    @Test
    void testGenerateAutologinKey_WhenSelectedIsNotNull_ShouldGenerateKey() {
        // Given
        backingBean.setSelected(testUser);

        // When
        backingBean.generateAutologinKey();

        // Then
        assertNotNull(testUser.getAutologinKey());
        assertEquals(35, testUser.getAutologinKey().length());
    }

    @Test
    void testGenerateAutologinKey_WhenSelectedIsNull_ShouldDoNothing() {
        // Given
        backingBean.setSelected(null);

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> backingBean.generateAutologinKey());
    }

    @Test
    void testGenerateAutologinKey_ShouldGenerateDifferentKeys() {
        // Given
        backingBean.setSelected(testUser);

        // When
        backingBean.generateAutologinKey();
        String key1 = testUser.getAutologinKey();

        backingBean.generateAutologinKey();
        String key2 = testUser.getAutologinKey();

        // Then
        assertNotEquals(key1, key2);
    }

    @Test
    void testHasRememberMeEntries_WhenEntriesExist_ShouldReturnTrue() {
        // Given
        String username = "test@example.com";
        List<MyRememberMe> entries = Arrays.asList(new MyRememberMe());

        when(rememberMeRepo.findAllByUsername(username)).thenReturn(entries);

        // When
        boolean result = backingBean.hasRememberMeEntries(username);

        // Then
        assertTrue(result);
        verify(rememberMeRepo, times(1)).findAllByUsername(username);
    }

    @Test
    void testHasRememberMeEntries_WhenNoEntriesExist_ShouldReturnFalse() {
        // Given
        String username = "test@example.com";
        when(rememberMeRepo.findAllByUsername(username)).thenReturn(new ArrayList<>());

        // When
        boolean result = backingBean.hasRememberMeEntries(username);

        // Then
        assertFalse(result);
        verify(rememberMeRepo, times(1)).findAllByUsername(username);
    }

    @Test
    void testDeleteRememberMeForUser_ShouldDeleteAndReload() {
        // Given
        String username = "test@example.com";
        when(repo.findAll()).thenReturn(new ArrayList<>());
        when(rememberMeRepo.findAll()).thenReturn(new ArrayList<>());

        // When
        backingBean.deleteRememberMeForUser(username);

        // Then
        verify(rememberMeRepo, times(1)).deleteAllByUsername(username);
        verify(repo, times(1)).findAll();
        verify(rememberMeRepo, times(1)).findAll();
    }

    @Test
    void testGenerateAutologinKey_ShouldContainOnlyValidCharacters() {
        // Given
        backingBean.setSelected(testUser);

        // When
        backingBean.generateAutologinKey();

        // Then
        String key = testUser.getAutologinKey();
        assertTrue(key.matches("[A-Za-z0-9]+"),
            "Autologin key should only contain alphanumeric characters");
    }
}
