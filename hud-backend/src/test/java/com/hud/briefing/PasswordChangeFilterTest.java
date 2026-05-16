package com.hud.briefing;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Tag("unit")
class PasswordChangeFilterTest {

    @Mock private UserRepository userRepository;
    @Mock private FilterChain chain;
    private PasswordChangeFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new PasswordChangeFilter(userRepository);
        AppUser user = new AppUser("admin", "hash", "ROLE_ADMIN");
        user.setPasswordChangeRequired(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "x", List.of()));
    }

    @Test
    void blocksProtectedPathWhenChangeRequired() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/news"), response, chain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void allowsPasswordEndpointWhenChangeRequired() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("PUT", "/api/auth/password"), response, chain);
        assertEquals(SC_OK, response.getStatus());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void allowsRequestWhenUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/news"), response, chain);
        assertEquals(SC_OK, response.getStatus());
        verify(chain).doFilter(any(), any());
        verify(userRepository, never()).findByUsername(any());
    }
}
