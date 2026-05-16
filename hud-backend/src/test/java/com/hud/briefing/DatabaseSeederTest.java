package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Tag("unit")
class DatabaseSeederTest {

    @Mock private LlmConfigRepository llmRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private DatabaseSeeder seeder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(passwordEncoder.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
        seeder = new DatabaseSeeder(llmRepository, userRepository, passwordEncoder);
    }

    @Test
    void seedsAdminWithProvidedPasswordAndRequiresChange() {
        when(userRepository.count()).thenReturn(0L);
        seeder.setAdminPassword("Configured123Pass");

        seeder.seedDefaultUser();

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertEquals("admin", saved.getUsername());
        assertEquals("hash:Configured123Pass", saved.getPassword());
        assertTrue(saved.isPasswordChangeRequired());
    }

    @Test
    void seedsRandomPasswordWhenNoneConfigured() {
        when(userRepository.count()).thenReturn(0L);
        seeder.setAdminPassword("");

        seeder.seedDefaultUser();

        ArgumentCaptor<String> pwCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(pwCaptor.capture());
        assertTrue(pwCaptor.getValue().length() >= 16, "random password should be >= 16 chars");
        assertNotEquals("admin", pwCaptor.getValue());
    }

    @Test
    void doesNotSeedWhenUsersExist() {
        when(userRepository.count()).thenReturn(1L);
        seeder.seedDefaultUser();
        verify(userRepository, never()).save(any());
    }
}
