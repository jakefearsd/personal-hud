package com.hud.analytics;

import com.hud.briefing.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class FullIntelligenceLifecycleE2E {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("hudtest")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DailyBriefingRepository briefingRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCompleteFullAdminLifecycle() {
        String baseUrl = "http://localhost:" + port;

        // 1. Verify Anonymous Access (Forbidden/Unauthorized)
        ResponseEntity<String> unauthorizedResp = restTemplate.postForEntity(baseUrl + "/api/briefings/trigger", null, String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, unauthorizedResp.getStatusCode(), "Anonymous trigger should be blocked");

        // 2. Perform Admin Login
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> loginRequest = new HttpEntity<>("username=admin&password=admin", headers);
        
        ResponseEntity<Map> loginResp = restTemplate.postForEntity(baseUrl + "/api/auth/login", loginRequest, Map.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode(), "Admin should be able to login");
        
        List<String> cookies = loginResp.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(cookies, "Login should return session cookie");
        String sessionCookie = cookies.get(0).split(";")[0];

        // 3. Trigger Briefing Pipeline (Admin Access)
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.set(HttpHeaders.COOKIE, sessionCookie);
        HttpEntity<Void> triggerEntity = new HttpEntity<>(authHeaders);
        
        ResponseEntity<String> triggerResp = restTemplate.postForEntity(baseUrl + "/api/briefings/trigger", triggerEntity, String.class);
        assertEquals(HttpStatus.OK, triggerResp.getStatusCode(), "Admin should be able to trigger pipeline");

        // 4. Verify Intelligence Persistence (Wait for background job to finish one item)
        // Since we hammer local model, we wait up to 2 minutes for at least one record
        boolean success = false;
        for (int i = 0; i < 24; i++) { // 2 minutes
            if (briefingRepository.count() > 0) {
                success = true;
                break;
            }
            try { Thread.sleep(5000); } catch (InterruptedException e) {}
        }
        
        assertTrue(success, "At least one briefing should have been generated and persisted");

        // 5. Verify Public Read-Only Access
        ResponseEntity<List> publicResp = restTemplate.getForEntity(baseUrl + "/api/briefings/latest", List.class);
        assertEquals(HttpStatus.OK, publicResp.getStatusCode(), "Public should see latest briefings");
        assertFalse(publicResp.getBody().isEmpty(), "Public feed should contain generated intel");

        // 6. Test Password Rotation
        HttpHeaders rotateHeaders = new HttpHeaders();
        rotateHeaders.set(HttpHeaders.COOKIE, sessionCookie);
        rotateHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> pwdRequest = Map.of("newPassword", "rotatetestpass");
        HttpEntity<Map> rotateEntity = new HttpEntity<>(pwdRequest, rotateHeaders);
        
        ResponseEntity<Map> rotateResp = restTemplate.exchange(baseUrl + "/api/auth/password", HttpMethod.PUT, rotateEntity, Map.class);
        assertEquals(HttpStatus.OK, rotateResp.getStatusCode(), "Admin should be able to rotate password");

        // 7. Verify Re-authentication with new credentials
        HttpEntity<String> newLoginReq = new HttpEntity<>("username=admin&password=rotatetestpass", headers);
        ResponseEntity<Map> newLoginResp = restTemplate.postForEntity(baseUrl + "/api/auth/login", newLoginReq, Map.class);
        assertEquals(HttpStatus.OK, newLoginResp.getStatusCode(), "Should be able to login with new rotated password");
    }
}
