package com.hud.briefing;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, PasswordChangeFilter passwordChangeFilter, CsrfCookieFilter csrfCookieFilter) throws Exception {
        http
            .addFilterAfter(passwordChangeFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(csrfCookieFilter, org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class)
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler())
            )
            .authorizeHttpRequests(auth -> auth
                // Public Endpoints & Static Resources
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg").permitAll()
                // Frontend Routes (SPA)
                .requestMatchers("/news/**", "/theaters", "/investments", "/config", "/observability").permitAll()
                
                // Public API Endpoints
                .requestMatchers(HttpMethod.GET, "/api/news", "/api/briefings/latest", "/api/investments/vitals", "/api/investments/macro-pods", "/api/investments/history/**", "/api/investments/events/**").permitAll()
                .requestMatchers("/api/auth/status").permitAll()
                
                // Admin-Only Endpoints
                .requestMatchers("/api/briefings/trigger", "/api/investments/trigger", "/api/investments/correlate", "/api/investments/sync", "/api/investments/predictions/trigger").hasRole("ADMIN")
                .requestMatchers("/api/config", "/api/config/**").hasRole("ADMIN")
                .requestMatchers("/api/pipelines", "/api/pipelines/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> {
                    logger.info("Login SUCCESS for user: {}", auth.getName());
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"status\":\"success\"}");
                })
                .failureHandler((req, res, exp) -> {
                    logger.warn("Login FAILURE for user: {} - {}", req.getParameter("username"), exp.getMessage());
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized\"}");
                })
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authExp) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"status\":\"error\",\"message\":\"Authentication required\"}");
                })
            );

        return http.build();
    }
}
