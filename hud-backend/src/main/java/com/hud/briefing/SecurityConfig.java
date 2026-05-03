package com.hud.briefing;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disabled for PoC simplicity with API
            .authorizeHttpRequests(auth -> auth
                // Public Endpoints
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news", "/api/briefings/latest", "/api/investments/vitals", "/api/investments/history/**", "/api/investments/events/**").permitAll()
                .requestMatchers("/api/auth/status").permitAll()
                
                // Admin-Only Endpoints
                .requestMatchers("/api/briefings/trigger", "/api/investments/trigger", "/api/investments/correlate").hasRole("ADMIN")
                .requestMatchers("/api/config", "/api/config/**").hasRole("ADMIN")
                .requestMatchers("/api/pipelines", "/api/pipelines/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> {
                    res.setStatus(HttpServletResponse.SC_OK);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"status\":\"success\"}");
                })
                .failureHandler((req, res, exp) -> {
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
