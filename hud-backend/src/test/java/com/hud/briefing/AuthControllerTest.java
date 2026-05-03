package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Tag("unit")
class AuthControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private Authentication authentication;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authController = new AuthController(userRepository, passwordEncoder);
    }

    @Test
    void shouldReturnAuthenticatedStatusForAdmin() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();

        Map<String, Object> status = authController.getStatus(authentication);

        assertTrue((Boolean) status.get("authenticated"));
        assertTrue((Boolean) status.get("isAdmin"));
        assertEquals("admin", status.get("username"));
    }

    @Test
    void shouldReturnUnauthenticatedStatusWhenNull() {
        Map<String, Object> status = authController.getStatus(null);
        assertFalse((Boolean) status.get("authenticated"));
        assertFalse((Boolean) status.get("isAdmin"));
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        
        AppUser user = new AppUser("admin", "oldHash", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("newHash");

        Map<String, String> request = Map.of("newPassword", "newSecurePass");
        Map<String, String> response = authController.changePassword(request, authentication);

        assertEquals("success", response.get("status"));
        assertEquals("newHash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowExceptionWhenChangingPasswordTooShort() {
        when(authentication.isAuthenticated()).thenReturn(true);
        Map<String, String> request = Map.of("newPassword", "123");

        assertThrows(IllegalArgumentException.class, () -> authController.changePassword(request, authentication));
    }
}
