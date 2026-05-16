package com.hud.briefing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * While the authenticated user has passwordChangeRequired=true, every request
 * except the auth endpoints needed to inspect status and set a new password
 * is rejected with 403.
 */
@Component
public class PasswordChangeFilter extends OncePerRequestFilter {

    private static final Set<String> EXEMPT_PATHS =
            Set.of("/api/auth/status", "/api/auth/password", "/api/auth/logout");

    private final UserRepository userRepository;

    public PasswordChangeFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        if (auth != null && auth.isAuthenticated() && !EXEMPT_PATHS.contains(path)) {
            // Fail-open: a missing user record has no password to change. This is a
            // UX gate only -- authorization is enforced by Spring Security separately.
            boolean mustChange = userRepository.findByUsername(auth.getName())
                    .map(AppUser::isPasswordChangeRequired)
                    .orElse(false);
            if (mustChange) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":\"error\",\"message\":\"Password change required\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
