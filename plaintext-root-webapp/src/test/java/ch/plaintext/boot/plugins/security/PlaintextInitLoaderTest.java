/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security;

import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import ch.plaintext.boot.plugins.security.persistence.MyUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for PlaintextInitLoader - initial user creation on startup.
 */
@ExtendWith(MockitoExtension.class)
class PlaintextInitLoaderTest {

    @Mock
    private MyUserRepository userRepository;

    @InjectMocks
    private PlaintextInitLoader initLoader;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        // No special setup needed
    }

    @Test
    void init_shouldCreateDefaultUser_whenNotExists() {
        when(userRepository.findByUsername("daniel.marthaler@plaintext.ch")).thenReturn(null);
        when(userRepository.save(any(MyUserEntity.class))).thenAnswer(i -> i.getArgument(0));

        initLoader.init();

        ArgumentCaptor<MyUserEntity> captor = ArgumentCaptor.forClass(MyUserEntity.class);
        verify(userRepository).save(captor.capture());

        MyUserEntity savedUser = captor.getValue();
        assertEquals("daniel.marthaler@plaintext.ch", savedUser.getUsername());
        assertTrue(savedUser.getRoles().contains("admin"));
        assertTrue(savedUser.getRoles().contains("root"));
        assertTrue(savedUser.getRoles().contains("user"));
        assertNotNull(savedUser.getPassword());
        assertTrue(encoder.matches("admin", savedUser.getPassword()));
        assertEquals("fHySOUPZo1N1zLOpviHmBukjSQUL1ivLkeM", savedUser.getAutologinKey());
    }

    @Test
    void init_shouldNotCreateUser_whenAlreadyExists() {
        MyUserEntity existing = new MyUserEntity();
        existing.setUsername("daniel.marthaler@plaintext.ch");
        when(userRepository.findByUsername("daniel.marthaler@plaintext.ch")).thenReturn(existing);

        initLoader.init();

        verify(userRepository, never()).save(any());
    }

    @Test
    void createRootUserDelayed_shouldCreateRootUser_whenNotExists() {
        when(userRepository.findByUsername("root@root.root")).thenReturn(null);
        when(userRepository.save(any(MyUserEntity.class))).thenAnswer(i -> i.getArgument(0));

        initLoader.createRootUserDelayed();

        ArgumentCaptor<MyUserEntity> captor = ArgumentCaptor.forClass(MyUserEntity.class);
        verify(userRepository).save(captor.capture());

        MyUserEntity rootUser = captor.getValue();
        assertEquals("root@root.root", rootUser.getUsername());
        assertTrue(rootUser.getRoles().contains("root"));
        assertTrue(rootUser.getRoles().contains("admin"));
        assertTrue(rootUser.getRoles().contains("user"));
        assertTrue(encoder.matches("root", rootUser.getPassword()));
    }

    @Test
    void createRootUserDelayed_shouldNotCreateRootUser_whenAlreadyExists() {
        MyUserEntity existing = new MyUserEntity();
        existing.setUsername("root@root.root");
        when(userRepository.findByUsername("root@root.root")).thenReturn(existing);

        initLoader.createRootUserDelayed();

        verify(userRepository, never()).save(any());
    }

    @Test
    void init_shouldSetMandatOnDefaultUser() {
        when(userRepository.findByUsername("daniel.marthaler@plaintext.ch")).thenReturn(null);
        when(userRepository.save(any(MyUserEntity.class))).thenAnswer(i -> i.getArgument(0));

        initLoader.init();

        ArgumentCaptor<MyUserEntity> captor = ArgumentCaptor.forClass(MyUserEntity.class);
        verify(userRepository).save(captor.capture());

        MyUserEntity savedUser = captor.getValue();
        assertEquals("plaintext", savedUser.getMandat());
    }

    @Test
    void createRootUserDelayed_shouldSetMandatToDefault() {
        when(userRepository.findByUsername("root@root.root")).thenReturn(null);
        when(userRepository.save(any(MyUserEntity.class))).thenAnswer(i -> i.getArgument(0));

        initLoader.createRootUserDelayed();

        ArgumentCaptor<MyUserEntity> captor = ArgumentCaptor.forClass(MyUserEntity.class);
        verify(userRepository).save(captor.capture());

        MyUserEntity rootUser = captor.getValue();
        assertEquals("default", rootUser.getMandat());
    }
}
