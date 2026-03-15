package ch.plaintext.boot.plugins.security;

import ch.plaintext.menuesteuerung.model.MandateMenuConfig;
import ch.plaintext.menuesteuerung.persistence.MandateMenuConfigRepository;
import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import ch.plaintext.boot.plugins.security.persistence.MyUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for PlaintextSecurityImpl - the core security component.
 */
@ExtendWith(MockitoExtension.class)
class PlaintextSecurityImplTest {

    @Mock
    private MyUserRepository userRepository;

    @Mock
    private MandateMenuConfigRepository mandateMenuConfigRepository;

    @InjectMocks
    private PlaintextSecurityImpl plaintextSecurity;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        // Setup security context mock
        SecurityContextHolder.setContext(securityContext);

        // Stub mandateMenuConfigRepository to return empty list by default
        lenient().when(mandateMenuConfigRepository.findAll()).thenReturn(Collections.emptyList());

        // Manually trigger PostConstruct using reflection
        try {
            java.lang.reflect.Method initMethod = PlaintextSecurityImpl.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(plaintextSecurity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PlaintextSecurityImpl", e);
        }
    }

    // ==================== getMandat() Tests ====================

    @Test
    void getMandat_shouldReturnMandatFromRoles_whenMandatRoleExists() {
        // Given: User has a mandat role
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("PROPERTY_MANDAT_dev"),
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_123")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When
        String mandat = plaintextSecurity.getMandat();

        // Then
        assertEquals("dev", mandat);
    }

    @Test
    void getMandat_shouldReturnDefault_whenNoMandatRoleExists() {
        // Given: User has no mandat role
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_123")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When
        String mandat = plaintextSecurity.getMandat();

        // Then
        assertEquals("default", mandat);
    }

    @Test
    void getMandat_shouldReturnError_whenAuthenticationIsNull() {
        // Given: No authentication
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        String mandat = plaintextSecurity.getMandat();

        // Then
        assertEquals("ERROR", mandat);
    }

    @Test
    void getMandat_shouldHandleMandatWithMultipleUnderscores() {
        // Given: Mandat with multiple underscores
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("PROPERTY_MANDAT_some_complex_mandat")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When
        String mandat = plaintextSecurity.getMandat();

        // Then
        assertEquals("mandat", mandat); // Last part after split
    }

    @Test
    void getMandat_shouldBeCaseInsensitive() {
        // Given: Uppercase MANDAT role
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("PROPERTY_MANDAT_PRODUCTION")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When
        String mandat = plaintextSecurity.getMandat();

        // Then
        assertEquals("production", mandat);
    }

    // ==================== getAllMandate() Tests ====================

    @Test
    void getAllMandate_shouldReturnAllUniqueMandateFromUsers() {
        // Given: Multiple users with different mandates
        MyUserEntity user1 = new MyUserEntity();
        user1.setMandat("dev");

        MyUserEntity user2 = new MyUserEntity();
        user2.setMandat("prod");

        MyUserEntity user3 = new MyUserEntity();
        user3.setMandat("dev"); // Duplicate

        List<MyUserEntity> users = Arrays.asList(user1, user2, user3);
        when(userRepository.findAll()).thenReturn(users);

        // When
        Set<String> mandante = plaintextSecurity.getAllMandate();

        // Then
        assertEquals(2, mandante.size());
        assertTrue(mandante.contains("dev"));
        assertTrue(mandante.contains("prod"));
    }

    @Test
    void getAllMandate_shouldIgnoreEmptyAndNullMandates() {
        // Given: Users with empty/null mandates
        MyUserEntity user1 = new MyUserEntity();
        user1.setMandat("dev");

        MyUserEntity user2 = new MyUserEntity();
        user2.setMandat(null);

        MyUserEntity user3 = new MyUserEntity();
        user3.setMandat("  ");

        MyUserEntity user4 = new MyUserEntity();
        user4.setMandat("");

        List<MyUserEntity> users = Arrays.asList(user1, user2, user3, user4);
        when(userRepository.findAll()).thenReturn(users);

        // When
        Set<String> mandante = plaintextSecurity.getAllMandate();

        // Then
        assertEquals(1, mandante.size());
        assertTrue(mandante.contains("dev"));
    }

    @Test
    void getAllMandate_shouldReturnDefaultWhenNoUsersFound() {
        // Given: Empty user list
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        Set<String> mandante = plaintextSecurity.getAllMandate();

        // Then
        assertEquals(1, mandante.size());
        assertTrue(mandante.contains("default"));
    }

    @Test
    void getAllMandate_shouldReturnDefaultOnDatabaseError() {
        // Given: Database error
        when(userRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // When
        Set<String> mandante = plaintextSecurity.getAllMandate();

        // Then
        assertEquals(1, mandante.size());
        assertTrue(mandante.contains("default"));
    }

    @Test
    void getAllMandate_shouldNormalizeMandatesToLowercase() {
        // Given: Users with mixed case mandates
        MyUserEntity user1 = new MyUserEntity();
        user1.setMandat("DEV");

        MyUserEntity user2 = new MyUserEntity();
        user2.setMandat("Dev");

        MyUserEntity user3 = new MyUserEntity();
        user3.setMandat("dev");

        List<MyUserEntity> users = Arrays.asList(user1, user2, user3);
        when(userRepository.findAll()).thenReturn(users);

        // When
        Set<String> mandante = plaintextSecurity.getAllMandate();

        // Then
        assertEquals(1, mandante.size()); // All should be normalized to "dev"
        assertTrue(mandante.contains("dev"));
    }

    // ==================== setMandat() Tests ====================

    @Test
    void setMandat_shouldUpdateSecurityContextAndDatabase() {
        // Given: Authenticated user with existing mandat
        List<GrantedAuthority> authorities = new ArrayList<>(Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("PROPERTY_MANDAT_old"),
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_123")
        ));
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        MyUserEntity user = new MyUserEntity();
        user.setId(123L);
        user.setMandat("old");
        when(userRepository.findById(123L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(MyUserEntity.class))).thenReturn(user);

        // When
        plaintextSecurity.setMandat("new");

        // Then: Verify database update
        verify(userRepository).findById(123L);
        verify(userRepository).save(argThat(u -> "new".equals(u.getMandat())));

        // Then: Verify security context update
        verify(securityContext).setAuthentication(argThat(newAuth ->
            newAuth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PROPERTY_MANDAT_new"))
        ));
    }

    @Test
    void setMandat_shouldRemoveOldMandatRole() {
        // Given: User with existing mandat
        List<GrantedAuthority> authorities = new ArrayList<>(Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("PROPERTY_MANDAT_old1"),
                new SimpleGrantedAuthority("PROPERTY_MANDAT_old2"), // Multiple old mandats
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_123")
        ));
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        MyUserEntity user = new MyUserEntity();
        user.setId(123L);
        when(userRepository.findById(123L)).thenReturn(Optional.of(user));

        // When
        plaintextSecurity.setMandat("new");

        // Then: Verify old mandat roles are removed
        verify(securityContext).setAuthentication(argThat(newAuth -> {
            long mandatCount = newAuth.getAuthorities().stream()
                .filter(a -> a.getAuthority().toLowerCase().contains("mandat"))
                .count();
            return mandatCount == 1; // Only one mandat role should remain
        }));
    }

    @Test
    void setMandat_shouldDoNothingWhenNoAuthentication() {
        // Given: No authentication
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        plaintextSecurity.setMandat("new");

        // Then: Nothing should be saved
        verify(userRepository, never()).save(any());
        verify(securityContext, never()).setAuthentication(any());
    }

    @Test
    void setMandat_shouldNotPersistWhenUserNotFound() {
        // Given: Authentication exists but user not in DB
        List<GrantedAuthority> authorities = new ArrayList<>(Arrays.asList(
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_999")
        ));
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        plaintextSecurity.setMandat("new");

        // Then: Should update context but not save to DB
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void setMandat_shouldNormalizeMandatToLowercase() {
        // Given
        List<GrantedAuthority> authorities = new ArrayList<>(Arrays.asList(
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_123")
        ));
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        MyUserEntity user = new MyUserEntity();
        user.setId(123L);
        when(userRepository.findById(123L)).thenReturn(Optional.of(user));

        // When
        plaintextSecurity.setMandat("UPPERCASE");

        // Then: Should be stored as lowercase
        verify(securityContext).setAuthentication(argThat(newAuth ->
            newAuth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PROPERTY_MANDAT_uppercase"))
        ));
    }

    // ==================== getId() Tests ====================

    @Test
    void getId_shouldReturnUserIdFromRoles() {
        // Given: User with myuserid role
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_12345")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When
        Long id = plaintextSecurity.getId();

        // Then
        assertEquals(12345L, id);
    }

    @Test
    void getId_shouldReturnMinusOneWhenNoUserIdRole() {
        // Given: User without myuserid role
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When
        Long id = plaintextSecurity.getId();

        // Then
        assertEquals(-1L, id);
    }

    @Test
    void getId_shouldExtractOnlyDigitsFromRole() {
        // Given: Role with mixed alphanumeric
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_abc123xyz")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When
        Long id = plaintextSecurity.getId();

        // Then
        assertEquals(123L, id);
    }

    @Test
    void getId_shouldReturnMinusOneOnError() {
        // Given: Null authentication
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        Long id = plaintextSecurity.getId();

        // Then
        assertEquals(-1L, id);
    }

    @Test
    void getId_shouldBeCaseInsensitive() {
        // Given: Uppercase role
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("PROPERTY_MYUSERID_999")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When
        Long id = plaintextSecurity.getId();

        // Then
        assertEquals(999L, id);
    }

    // ==================== getUser() Tests ====================

    @Test
    void getUser_shouldReturnUsername() {
        // Given: Authenticated user
        Authentication auth = new UsernamePasswordAuthenticationToken("john.doe@example.com", "password", java.util.Collections.emptyList());
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When
        String username = plaintextSecurity.getUser();

        // Then
        assertEquals("john.doe@example.com", username);
    }

    @Test
    void getUser_shouldReturnErrorOnException() {
        // Given: Null authentication
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        String username = plaintextSecurity.getUser();

        // Then
        assertEquals("SYSTEM", username);
    }

    // ==================== getMandatForUser() Tests ====================

    @Test
    void getMandatForUser_shouldReturnMandatFromDatabase() {
        // Given: User exists in database
        MyUserEntity user = new MyUserEntity();
        user.setId(123L);
        user.setMandat("production");
        when(userRepository.findById(123L)).thenReturn(Optional.of(user));

        // When
        String mandat = plaintextSecurity.getMandatForUser(123L);

        // Then
        assertEquals("production", mandat);
    }

    @Test
    void getMandatForUser_shouldReturnNullWhenUserNotFound() {
        // Given: User doesn't exist
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        String mandat = plaintextSecurity.getMandatForUser(999L);

        // Then
        assertNull(mandat);
    }

    @Test
    void getMandatForUser_shouldReturnNullOnDatabaseError() {
        // Given: Database error
        when(userRepository.findById(any())).thenThrow(new RuntimeException("Database error"));

        // When
        String mandat = plaintextSecurity.getMandatForUser(123L);

        // Then
        assertNull(mandat);
    }

    // ==================== ifGranted() Tests ====================

    @Test
    void ifGranted_shouldReturnTrueWhenRoleExists() {
        // Given: User has the role
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When & Then
        assertTrue(plaintextSecurity.ifGranted("ADMIN"));
        assertTrue(plaintextSecurity.ifGranted("USER"));
    }

    @Test
    void ifGranted_shouldReturnFalseWhenRoleDoesNotExist() {
        // Given: User doesn't have the role
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When & Then
        assertFalse(plaintextSecurity.ifGranted("ADMIN"));
    }

    @Test
    void ifGranted_shouldHandleRoleWithOrWithoutPrefix() {
        // Given: User has role
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When & Then
        assertTrue(plaintextSecurity.ifGranted("ADMIN"));
        assertTrue(plaintextSecurity.ifGranted("ROLE_ADMIN"));
    }

    @Test
    void ifGranted_shouldReturnFalseForNullRole() {
        // Given: Any authentication (no need to stub as null check happens first)
        // When & Then
        assertFalse(plaintextSecurity.ifGranted(null));
    }

    @Test
    void ifGranted_shouldBeCaseInsensitive() {
        // Given: User has role in uppercase
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When & Then
        assertTrue(plaintextSecurity.ifGranted("admin"));
        assertTrue(plaintextSecurity.ifGranted("Admin"));
        assertTrue(plaintextSecurity.ifGranted("ADMIN"));
    }

    // ==================== getAuthentication() Tests ====================

    @Test
    void getAuthentication_shouldReturnCurrentAuthentication() {
        // Given: Authentication exists
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", "password");
        when(securityContext.getAuthentication()).thenReturn(auth);

        // When
        Authentication result = plaintextSecurity.getAuthentication();

        // Then
        assertSame(auth, result);
    }

    @Test
    void getAuthentication_shouldReturnNullWhenNoAuthentication() {
        // Given: No authentication
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        Authentication result = plaintextSecurity.getAuthentication();

        // Then
        assertNull(result);
    }
}
