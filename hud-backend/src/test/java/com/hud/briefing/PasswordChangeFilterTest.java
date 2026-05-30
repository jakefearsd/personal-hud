package com.hud.briefing;

import jakarta.servlet.FilterChain;
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

    private MockHttpServletRequest createRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    @Test
    void blocksProtectedPathWhenChangeRequired() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(createRequest("GET", "/api/news"), response, chain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void allowsPasswordEndpointWhenChangeRequired() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(createRequest("PUT", "/api/auth/password"), response, chain);
        assertEquals(SC_OK, response.getStatus());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void allowsStatusEndpointWhenChangeRequired() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(createRequest("GET", "/api/auth/status"), response, chain);
        assertEquals(SC_OK, response.getStatus());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void allowsLogoutEndpointWhenChangeRequired() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(createRequest("POST", "/api/auth/logout"), response, chain);
        assertEquals(SC_OK, response.getStatus());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void allowsProtectedPathWhenChangeNotRequired() throws Exception {
        AppUser user = new AppUser("admin", "hash", "ROLE_ADMIN");
        user.setPasswordChangeRequired(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(createRequest("GET", "/api/news"), response, chain);
        assertEquals(SC_OK, response.getStatus());
        verify(chain).doFilter(any(), any());
    }
}
