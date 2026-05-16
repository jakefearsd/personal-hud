# HUD Phase 1 — Stabilization & Security Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the release-blocking security and reliability gaps in HUD so a fresh deployment is safe by default and `mvn test` is green without external services.

**Architecture:** Incremental hardening of the existing Spring Boot backend and React frontend. Schema comes under Flyway control first, then all other changes build on it. No new subsystems; existing patterns (constructor injection, `@Tag`-ed tests, strategy/factory structure) are preserved.

**Tech Stack:** Spring Boot 3.2.5, Java 21, Maven, PostgreSQL, Flyway, Spring Security, JUnit 5, Mockito, Testcontainers, React 19 / Vite / TypeScript, Vitest.

**Source spec:** `docs/superpowers/specs/2026-05-16-hud-stabilization-breadth-design.md` (Part A, items A1–A10).

---

## File Structure

| File | Responsibility | Tasks |
|------|----------------|-------|
| `.gitignore` | Ignore data/secret files | 1 |
| `hud-backend/pom.xml` | Flyway dependency, Surefire config | 2, 14 |
| `pom.xml` | `integration` Maven profile | 14 |
| `hud-backend/src/main/resources/db/migration/V1__baseline.sql` | Baseline schema | 2 |
| `hud-backend/src/main/resources/db/migration/V2__add_password_change_required.sql` | New column | 6 |
| `hud-backend/src/main/resources/application.yml` | Flyway + JPA prod config | 2, 3 |
| `hud-backend/src/test/resources/application-test.yml` | Disable Flyway for H2 tests | 2 |
| `hud-backend/src/test/java/com/hud/MigrationIntegrationTest.java` | Migration drift test | 2 |
| `hud-backend/src/main/java/com/hud/AsyncConfig.java` | Named async executors | 5 |
| `hud-backend/src/main/java/com/hud/briefing/AppUser.java` | `passwordChangeRequired` field | 6 |
| `hud-backend/src/main/java/com/hud/briefing/DatabaseSeeder.java` | Random admin password | 7 |
| `hud-backend/src/main/java/com/hud/briefing/AuthController.java` | Password policy, status flag | 8 |
| `hud-backend/src/main/java/com/hud/briefing/PasswordChangeFilter.java` | Block users pending password change | 8 |
| `hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java` | CSRF, method security, filter wiring | 8, 10, 12 |
| `hud-frontend/src/api.ts` | CSRF-aware fetch wrapper | 11 |
| `hud-frontend/src/App.tsx` | Forced password-change routing | 9, 11 |
| `hud-frontend/src/components/ChangePasswordView.tsx` | Forced password-change UI | 9 |
| `hud-backend/.../PlaywrightScraperService.java` | Close `Page` resource | 4 |
| `hud-backend/.../LlmConfigController.java` | Full API-key masking | 13 |
| Various `*Test.java` | `@Tag` integration tests | 14 |

Task order is dependency-driven: repo cleanup → Flyway → simple fixes → credentials → CSRF/auth → test gating.

---

## Task 1: Repository hygiene

**Files:**
- Modify: `.gitignore`
- Remove from tracking: root `*.csv`, `cookies.txt`, `bootstrap_global.sql`, `bootstrap_history.sql`

- [ ] **Step 1: Stop tracking data and secret files**

Run:
```bash
cd /home/jakefear/source/hud
git rm --cached cookies.txt bootstrap_global.sql bootstrap_history.sql
git rm --cached '*.csv' 2>/dev/null || true
git status --short
```
Expected: the listed files show as `D` (deleted from index); they remain on disk.

- [ ] **Step 2: Add ignore patterns**

Append to `.gitignore`:
```gitignore
# Data & generated artifacts (regenerated via harvest_*.py)
/*.csv
bootstrap_*.sql

# Local session cookies
cookies.txt
```

- [ ] **Step 3: Verify nothing tracked matches**

Run: `git ls-files | grep -E 'cookies.txt|bootstrap_.*\.sql|^[^/]*\.csv$' || echo CLEAN`
Expected: `CLEAN`

- [ ] **Step 4: Commit**

```bash
git add .gitignore
git commit -m "chore: stop tracking bulk data and session cookie files"
```

---

## Task 2: Flyway migrations + baseline

**Files:**
- Modify: `hud-backend/pom.xml`
- Modify: `hud-backend/src/main/resources/application.yml`
- Modify: `hud-backend/src/test/resources/application-test.yml`
- Create: `hud-backend/src/main/resources/db/migration/V1__baseline.sql`
- Create: `hud-backend/src/test/java/com/hud/MigrationIntegrationTest.java`

- [ ] **Step 1: Add the Flyway dependency**

In `hud-backend/pom.xml`, inside `<dependencies>`, after the `postgresql` dependency:
```xml
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
```
(Version is managed by the Spring Boot BOM.)

- [ ] **Step 2: Generate the baseline schema SQL**

The baseline must exactly match the current Hibernate-inferred schema. Generate it with Hibernate's schema exporter:
```bash
cd /home/jakefear/source/hud
mkdir -p hud-backend/src/main/resources/db/migration
docker compose up -d db
mvn -q -pl hud-backend spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dspring.flyway.enabled=false -Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create -Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=hud-backend/src/main/resources/db/migration/V1__baseline.sql -Dspring.jpa.hibernate.ddl-auto=none"
```
Let the app finish startup, then stop it with Ctrl+C. Hibernate writes every `create table` / constraint statement to `V1__baseline.sql`.

- [ ] **Step 3: Sanity-check the baseline file**

Run: `grep -c 'create table' hud-backend/src/main/resources/db/migration/V1__baseline.sql`
Expected: a count matching the JPA entities — 9 (`users`, `llm_configs`, `briefing_schedules`, `daily_briefings`, `pipeline_runs`, `macro_metrics`, `metric_history`, `market_predictions`, `market_events`). If the count differs, the app did not fully start; re-run Step 2.

- [ ] **Step 4: Switch production config to validate + Flyway**

In `hud-backend/src/main/resources/application.yml`, change the `jpa` block so `ddl-auto` is `validate`, and add a `flyway` block under `spring`:
```yaml
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
```

- [ ] **Step 5: Disable Flyway in the H2 test profile**

Replace `hud-backend/src/test/resources/application-test.yml` with:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
```

- [ ] **Step 6: Write the migration drift integration test**

Create `hud-backend/src/test/java/com/hud/MigrationIntegrationTest.java`:
```java
package com.hud;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the full context against a real Postgres so Flyway migrations run and
 * Hibernate ddl-auto=validate confirms the entities match the migrated schema.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest
class MigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Test
    void migrationsApplyAndSchemaValidates() {
        // Context loading is the assertion: Flyway migrates, then ddl-auto=validate
        // throws if the entities and migrated schema disagree.
    }
}
```

- [ ] **Step 7: Run the migration test**

Run: `mvn test -pl hud-backend -Dtest=MigrationIntegrationTest -DexcludedGroups=`
Expected: PASS. (`-DexcludedGroups=` overrides the exclusion added in Task 14; before Task 14 it is harmless.) If it fails with a Hibernate `SchemaManagementException`, the baseline in Step 2 is incomplete — regenerate it.

- [ ] **Step 8: Commit**

```bash
git add hud-backend/pom.xml hud-backend/src/main/resources/application.yml \
  hud-backend/src/test/resources/application-test.yml \
  hud-backend/src/main/resources/db/migration/V1__baseline.sql \
  hud-backend/src/test/java/com/hud/MigrationIntegrationTest.java
git commit -m "feat: bring schema under Flyway control with baseline migration"
```

---

## Task 3: Disable SQL logging by default

**Files:**
- Modify: `hud-backend/src/main/resources/application.yml`

- [ ] **Step 1: Turn off show-sql**

In `hud-backend/src/main/resources/application.yml`, change `show-sql: true` to `show-sql: false` under `spring.jpa`.

- [ ] **Step 2: Verify the app still starts**

Run: `mvn -q -pl hud-backend spring-boot:run` (with `docker compose up -d db` running), confirm startup completes with no SQL statements in the log, then Ctrl+C.
Expected: no `Hibernate:` SQL lines in output.

- [ ] **Step 3: Commit**

```bash
git add hud-backend/src/main/resources/application.yml
git commit -m "chore: disable SQL logging in default profile"
```

---

## Task 4: Fix Playwright Page resource leak

**Files:**
- Modify: `hud-backend/src/main/java/com/hud/news/PlaywrightScraperService.java:45-60`

- [ ] **Step 1: Wrap Page in try-with-resources**

In `PlaywrightScraperService.executeInBrowser()`, replace the `try (BrowserContext ...)` block body so `Page` is closed on every path:
```java
                try (BrowserContext context = browser.newContext(contextOptions);
                     Page page = context.newPage()) {
                    return task.execute(page);
                }
```
(`Page` implements `AutoCloseable`; closing it before the context is safe and explicit.)

- [ ] **Step 2: Compile to confirm the change is valid**

Run: `mvn -q -pl hud-backend compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Run the existing scraper unit tests**

Run: `mvn test -pl hud-backend -Dtest=PlaywrightScraperServiceUnitTest -DexcludedGroups=`
Expected: PASS (these cover `cleanExtractedText`, `truncateContent`, `isValidForDeepCrawl` — they confirm the file still behaves).

- [ ] **Step 4: Commit**

```bash
git add hud-backend/src/main/java/com/hud/news/PlaywrightScraperService.java
git commit -m "fix: close Playwright Page to prevent resource leak on failure paths"
```

---

## Task 5: Named async executors

**Files:**
- Create: `hud-backend/src/main/java/com/hud/AsyncConfig.java`
- Create: `hud-backend/src/test/java/com/hud/AsyncConfigTest.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/AutomatedBriefingService.java` (`@Async` annotations)

- [ ] **Step 1: Write the failing test**

Create `hud-backend/src/test/java/com/hud/AsyncConfigTest.java`:
```java
package com.hud;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    void briefingExecutorIsBounded() {
        ThreadPoolTaskExecutor executor = config.briefingExecutor();
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(4, executor.getMaxPoolSize());
        assertTrue(executor.getThreadNamePrefix().startsWith("hud-briefing-"));
    }

    @Test
    void scrapeExecutorIsBounded() {
        ThreadPoolTaskExecutor executor = config.scrapeExecutor();
        assertEquals(4, executor.getCorePoolSize());
        assertEquals(8, executor.getMaxPoolSize());
        assertTrue(executor.getThreadNamePrefix().startsWith("hud-scrape-"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -pl hud-backend -Dtest=AsyncConfigTest -DexcludedGroups=`
Expected: FAIL — `AsyncConfig` does not exist (compilation error).

- [ ] **Step 3: Create the AsyncConfig class**

Create `hud-backend/src/main/java/com/hud/AsyncConfig.java`:
```java
package com.hud;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Explicit bounded executors for @Async work. Long-running LLM tasks
 * must not accumulate on an unbounded default pool; CallerRunsPolicy
 * applies back-pressure instead of growing the queue without limit.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "briefingExecutor")
    public ThreadPoolTaskExecutor briefingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("hud-briefing-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "scrapeExecutor")
    public ThreadPoolTaskExecutor scrapeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("hud-scrape-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -pl hud-backend -Dtest=AsyncConfigTest -DexcludedGroups=`
Expected: PASS.

- [ ] **Step 5: Point briefing @Async methods at the named executor**

In `hud-backend/src/main/java/com/hud/briefing/AutomatedBriefingService.java`, change each of the three `@Async` annotations (on `generateForModel`, `generateDailyBriefing`, `generateForCategory`) to `@Async("briefingExecutor")`.

- [ ] **Step 6: Verify the context still loads**

Run: `mvn test -pl hud-backend -Dtest=HudBackendApplicationTests -DexcludedGroups=`
Expected: PASS — confirms the named executor bean resolves for `@Async`.

- [ ] **Step 7: Commit**

```bash
git add hud-backend/src/main/java/com/hud/AsyncConfig.java \
  hud-backend/src/test/java/com/hud/AsyncConfigTest.java \
  hud-backend/src/main/java/com/hud/briefing/AutomatedBriefingService.java
git commit -m "feat: add bounded named async executors with back-pressure"
```

---

## Task 6: Add `passwordChangeRequired` to AppUser

**Files:**
- Modify: `hud-backend/src/main/java/com/hud/briefing/AppUser.java`
- Create: `hud-backend/src/main/resources/db/migration/V2__add_password_change_required.sql`

- [ ] **Step 1: Add the field and accessors to AppUser**

In `hud-backend/src/main/java/com/hud/briefing/AppUser.java`, add the field after `role` and accessors after `setRole`:
```java
    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired = false;
```
```java
    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }
    public void setPasswordChangeRequired(boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }
```

- [ ] **Step 2: Create the migration**

Create `hud-backend/src/main/resources/db/migration/V2__add_password_change_required.sql`:
```sql
ALTER TABLE users
    ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
```

- [ ] **Step 3: Verify migration + entity agree**

Run: `mvn test -pl hud-backend -Dtest=MigrationIntegrationTest -DexcludedGroups=`
Expected: PASS — Flyway applies V1 + V2, `ddl-auto=validate` confirms the new column matches the entity.

- [ ] **Step 4: Commit**

```bash
git add hud-backend/src/main/java/com/hud/briefing/AppUser.java \
  hud-backend/src/main/resources/db/migration/V2__add_password_change_required.sql
git commit -m "feat: add passwordChangeRequired flag to AppUser"
```

---

## Task 7: Seed a random admin password

**Files:**
- Modify: `hud-backend/src/main/java/com/hud/briefing/DatabaseSeeder.java`
- Create: `hud-backend/src/test/java/com/hud/briefing/DatabaseSeederTest.java`

- [ ] **Step 1: Write the failing test**

Create `hud-backend/src/test/java/com/hud/briefing/DatabaseSeederTest.java`:
```java
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -pl hud-backend -Dtest=DatabaseSeederTest -DexcludedGroups=`
Expected: FAIL — `setAdminPassword` and `seedDefaultUser` are not accessible (the method is `private`, the setter does not exist).

- [ ] **Step 3: Update DatabaseSeeder**

In `hud-backend/src/main/java/com/hud/briefing/DatabaseSeeder.java`:

Add imports:
```java
import java.security.SecureRandom;
```

Add the field after `defaultNumCtx`:
```java
    @Value("${HUD_ADMIN_PASSWORD:}")
    private String adminPassword;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PW_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
```

Add a package-private setter (for tests) after the constructor:
```java
    void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
```

Replace `seedDefaultUser()` and make it package-private, and add the generator:
```java
    void seedDefaultUser() {
        if (userRepository.count() == 0) {
            String password = (adminPassword == null || adminPassword.isBlank())
                    ? generatePassword()
                    : adminPassword;
            boolean generated = (adminPassword == null || adminPassword.isBlank());

            AppUser admin = new AppUser("admin", passwordEncoder.encode(password), "ROLE_ADMIN");
            admin.setPasswordChangeRequired(true);
            userRepository.save(admin);

            if (generated) {
                logger.warn("Seeded admin user with a GENERATED password: {} "
                        + "-- change it on first login.", password);
            } else {
                logger.info("Seeded admin user from HUD_ADMIN_PASSWORD; "
                        + "password change is required on first login.");
            }
        }
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(PW_CHARS.charAt(RANDOM.nextInt(PW_CHARS.length())));
        }
        return sb.toString();
    }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -pl hud-backend -Dtest=DatabaseSeederTest -DexcludedGroups=`
Expected: PASS (all 3 tests).

- [ ] **Step 5: Commit**

```bash
git add hud-backend/src/main/java/com/hud/briefing/DatabaseSeeder.java \
  hud-backend/src/test/java/com/hud/briefing/DatabaseSeederTest.java
git commit -m "feat: seed admin with env or random password, require change on first login"
```

---

## Task 8: Password policy + forced-change gate

**Files:**
- Modify: `hud-backend/src/main/java/com/hud/briefing/AuthController.java`
- Modify: `hud-backend/src/test/java/com/hud/briefing/AuthControllerTest.java`
- Create: `hud-backend/src/main/java/com/hud/briefing/PasswordChangeFilter.java`
- Create: `hud-backend/src/test/java/com/hud/briefing/PasswordChangeFilterTest.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java`

- [ ] **Step 1: Update AuthController tests for the new policy**

In `hud-backend/src/test/java/com/hud/briefing/AuthControllerTest.java`:

The controller now needs the user looked up in `getStatus`. Update `shouldReturnAuthenticatedStatusForAdmin` to stub the repository and assert the new flag:
```java
    @Test
    void shouldReturnAuthenticatedStatusForAdmin() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        AppUser user = new AppUser("admin", "hash", "ROLE_ADMIN");
        user.setPasswordChangeRequired(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        Map<String, Object> status = authController.getStatus(authentication);

        assertTrue((Boolean) status.get("authenticated"));
        assertTrue((Boolean) status.get("isAdmin"));
        assertEquals("admin", status.get("username"));
        assertTrue((Boolean) status.get("passwordChangeRequired"));
    }
```

Replace `shouldThrowExceptionWhenChangingPasswordTooShort` to use a password under the new 12-char minimum, and update the two tests that use digit-free passwords so they satisfy the letter+digit rule:
```java
    @Test
    void shouldThrowExceptionWhenChangingPasswordTooShort() {
        when(authentication.isAuthenticated()).thenReturn(true);
        Map<String, String> request = Map.of("newPassword", "short1");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authController.changePassword(request, authentication));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldRejectPasswordWithoutDigit() {
        when(authentication.isAuthenticated()).thenReturn(true);
        Map<String, String> request = Map.of("newPassword", "alllettersnope");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authController.changePassword(request, authentication));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
```
In `shouldChangePasswordSuccessfully`, change the request to `Map.of("newPassword", "newSecurePass1")` and add `assertFalse(user.isPasswordChangeRequired());` after the existing assertions (the user starts with the flag set):
```java
        AppUser user = new AppUser("admin", "oldHash", "ROLE_ADMIN");
        user.setPasswordChangeRequired(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("newHash");

        Map<String, String> request = Map.of("newPassword", "newSecurePass1");
        Map<String, String> response = authController.changePassword(request, authentication);

        assertEquals("success", response.get("status"));
        assertEquals("newHash", user.getPassword());
        assertFalse(user.isPasswordChangeRequired());
        verify(userRepository).save(user);
```
In `shouldThrowUnauthorizedWhenNotAuthenticated` and `shouldThrowNotFoundWhenUserMissing`, change `"validPassword"` to `"validPassword1"`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn test -pl hud-backend -Dtest=AuthControllerTest -DexcludedGroups=`
Expected: FAIL — `passwordChangeRequired` not in status, weak passwords still accepted.

- [ ] **Step 3: Update AuthController**

Replace `hud-backend/src/main/java/com/hud/briefing/AuthController.java` body so it validates strength, exposes the flag, and clears it on change:
```java
package com.hud.briefing;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus(Authentication authentication) {
        Map<String, Object> status = new HashMap<>();
        if (authentication != null && authentication.isAuthenticated()) {
            status.put("authenticated", true);
            status.put("username", authentication.getName());
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            status.put("roles", roles);
            status.put("isAdmin", roles.contains("ROLE_ADMIN"));
            boolean mustChange = userRepository.findByUsername(authentication.getName())
                    .map(AppUser::isPasswordChangeRequired)
                    .orElse(false);
            status.put("passwordChangeRequired", mustChange);
        } else {
            status.put("authenticated", false);
            status.put("isAdmin", false);
            status.put("passwordChangeRequired", false);
        }
        return status;
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(@RequestBody Map<String, String> request,
                                              Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        validateStrength(request.get("newPassword"));

        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(request.get("newPassword")));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    private void validateStrength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must contain at least one letter and one digit");
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn test -pl hud-backend -Dtest=AuthControllerTest -DexcludedGroups=`
Expected: PASS (all tests, including the new `shouldRejectPasswordWithoutDigit`).

- [ ] **Step 5: Write the failing filter test**

Create `hud-backend/src/test/java/com/hud/briefing/PasswordChangeFilterTest.java`:
```java
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
}
```

- [ ] **Step 6: Run the filter test to verify it fails**

Run: `mvn test -pl hud-backend -Dtest=PasswordChangeFilterTest -DexcludedGroups=`
Expected: FAIL — `PasswordChangeFilter` does not exist.

- [ ] **Step 7: Create the filter**

Create `hud-backend/src/main/java/com/hud/briefing/PasswordChangeFilter.java`:
```java
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
        if (auth != null && auth.isAuthenticated() && !EXEMPT_PATHS.contains(request.getServletPath())) {
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
```

- [ ] **Step 8: Run the filter test to verify it passes**

Run: `mvn test -pl hud-backend -Dtest=PasswordChangeFilterTest -DexcludedGroups=`
Expected: PASS.

- [ ] **Step 9: Wire the filter into SecurityConfig**

In `hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java`, add imports:
```java
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
```
Inject the filter and register it. Change the `filterChain` method signature to accept the filter and add it after the security context is established:
```java
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           PasswordChangeFilter passwordChangeFilter) throws Exception {
        http
            .addFilterAfter(passwordChangeFilter, UsernamePasswordAuthenticationFilter.class)
```
(Keep the rest of the existing `http` builder chain unchanged.)

- [ ] **Step 10: Verify the context loads**

Run: `mvn test -pl hud-backend -Dtest=HudBackendApplicationTests -DexcludedGroups=`
Expected: PASS.

- [ ] **Step 11: Commit**

```bash
git add hud-backend/src/main/java/com/hud/briefing/AuthController.java \
  hud-backend/src/test/java/com/hud/briefing/AuthControllerTest.java \
  hud-backend/src/main/java/com/hud/briefing/PasswordChangeFilter.java \
  hud-backend/src/test/java/com/hud/briefing/PasswordChangeFilterTest.java \
  hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java
git commit -m "feat: enforce password strength and gate users pending password change"
```

---

## Task 9: Forced password-change UI

**Files:**
- Create: `hud-frontend/src/components/ChangePasswordView.tsx`
- Modify: `hud-frontend/src/App.tsx`

- [ ] **Step 1: Create the ChangePasswordView component**

Create `hud-frontend/src/components/ChangePasswordView.tsx`:
```tsx
import { useState } from 'react'

interface Props {
  onChanged: () => void
}

export function ChangePasswordView({ onChanged }: Props) {
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    if (password !== confirm) {
      setError('Passwords do not match')
      return
    }
    fetch('/api/auth/password', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ newPassword: password }),
    })
      .then(res => {
        if (res.ok) {
          onChanged()
        } else {
          res.json().then(d => setError(d.message || 'Password rejected'))
        }
      })
      .catch(err => setError(err.message))
  }

  return (
    <div className="change-password-view">
      <h2>Set a New Password</h2>
      <p>Your account requires a password change before you can continue.</p>
      <form onSubmit={submit}>
        <input
          type="password"
          placeholder="New password (min 12 chars, letter + digit)"
          value={password}
          onChange={e => setPassword(e.target.value)}
        />
        <input
          type="password"
          placeholder="Confirm password"
          value={confirm}
          onChange={e => setConfirm(e.target.value)}
        />
        {error && <div className="error-banner">{error}</div>}
        <button type="submit">Update Password</button>
      </form>
    </div>
  )
}
```

- [ ] **Step 2: Track the flag in App.tsx**

In `hud-frontend/src/App.tsx`:

Add the import near the other component imports:
```tsx
import { ChangePasswordView } from './components/ChangePasswordView'
```
Add state next to the other auth state:
```tsx
  const [passwordChangeRequired, setPasswordChangeRequired] = useState(false)
```
In `fetchAuthStatus`, set the flag from the response — replace the `.then(data => {...})` body:
```tsx
      .then(data => {
        setIsAuthenticated(data.authenticated)
        setIsAdmin(data.isAdmin)
        setUsername(data.username || '')
        setPasswordChangeRequired(data.passwordChangeRequired || false)
      })
```

- [ ] **Step 3: Render the forced view before the normal app**

In `hud-frontend/src/App.tsx`, add this block in the function body immediately **before** the existing `return (` statement, so it short-circuits rendering when a change is required:
```tsx
  if (isAuthenticated && passwordChangeRequired) {
    return (
      <div className="app-container">
        <ChangePasswordView onChanged={() => { setPasswordChangeRequired(false); fetchAuthStatus() }} />
      </div>
    )
  }
```

- [ ] **Step 4: Verify the frontend builds**

Run: `cd hud-frontend && npm run build`
Expected: `tsc -b` and `vite build` succeed with no type errors.

- [ ] **Step 5: Run the frontend tests**

Run: `cd hud-frontend && npx vitest run`
Expected: PASS (existing `App.test.tsx` still passes).

- [ ] **Step 6: Commit**

```bash
git add hud-frontend/src/components/ChangePasswordView.tsx hud-frontend/src/App.tsx
git commit -m "feat: force password-change view for users with pending change"
```

---

## Task 10: Re-enable CSRF (backend)

**Files:**
- Modify: `hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java`

- [ ] **Step 1: Replace the CSRF disable with a cookie token repository**

In `hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java`, add the import:
```java
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
```
Replace the line `.csrf(csrf -> csrf.disable())` with:
```java
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
```

- [ ] **Step 2: Verify the context loads**

Run: `mvn test -pl hud-backend -Dtest=HudBackendApplicationTests -DexcludedGroups=`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java
git commit -m "feat: re-enable CSRF protection with cookie token repository"
```

---

## Task 11: CSRF-aware fetch wrapper (frontend)

**Files:**
- Create: `hud-frontend/src/api.ts`
- Create: `hud-frontend/src/api.test.ts`
- Modify: `hud-frontend/src/App.tsx`
- Modify: `hud-frontend/src/components/ChangePasswordView.tsx`

- [ ] **Step 1: Write the failing test**

Create `hud-frontend/src/api.test.ts`:
```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { apiFetch } from './api'

describe('apiFetch', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=test-token-123'
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true }))
  })

  it('adds the X-XSRF-TOKEN header from the cookie on POST', async () => {
    await apiFetch('/api/briefings/trigger', { method: 'POST' })
    const [, init] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('test-token-123')
  })

  it('does not add the header on GET', async () => {
    await apiFetch('/api/news', { method: 'GET' })
    const [, init] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBeNull()
  })
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd hud-frontend && npx vitest run src/api.test.ts`
Expected: FAIL — `./api` does not exist.

- [ ] **Step 3: Create the wrapper**

Create `hud-frontend/src/api.ts`:
```ts
const MUTATING = new Set(['POST', 'PUT', 'DELETE', 'PATCH'])

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(^|;\\s*)' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[2]) : null
}

/**
 * fetch wrapper that attaches the CSRF token header on mutating requests.
 * Use for every POST/PUT/DELETE call to the HUD API.
 */
export function apiFetch(url: string, init: RequestInit = {}): Promise<Response> {
  const method = (init.method || 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (MUTATING.has(method)) {
    const token = readCookie('XSRF-TOKEN')
    if (token) headers.set('X-XSRF-TOKEN', token)
  }
  return fetch(url, { ...init, headers })
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd hud-frontend && npx vitest run src/api.test.ts`
Expected: PASS (both cases).

- [ ] **Step 5: Migrate mutating calls in App.tsx**

In `hud-frontend/src/App.tsx`, add to the imports:
```tsx
import { apiFetch } from './api'
```
In `triggerBriefing`, change `fetch('/api/briefings/trigger', { method: 'POST' })` to `apiFetch('/api/briefings/trigger', { method: 'POST' })`.
In `handleLogout`, change `fetch('/api/auth/logout', { method: 'POST' })` to `apiFetch('/api/auth/logout', { method: 'POST' })`.
(Leave the `GET` calls — `fetchAuthStatus`, `fetchConfigs`, `fetchLiveNews`, `fetchLatestBriefings` — on plain `fetch`.)

- [ ] **Step 6: Migrate the password change call**

In `hud-frontend/src/components/ChangePasswordView.tsx`, add `import { apiFetch } from '../api'` and change the `fetch('/api/auth/password', { method: 'PUT', ... })` call to `apiFetch(...)` with the same arguments.

- [ ] **Step 7: Build and test the frontend**

Run: `cd hud-frontend && npm run build && npx vitest run`
Expected: build succeeds, all tests PASS.

- [ ] **Step 8: Commit**

```bash
git add hud-frontend/src/api.ts hud-frontend/src/api.test.ts \
  hud-frontend/src/App.tsx hud-frontend/src/components/ChangePasswordView.tsx
git commit -m "feat: send CSRF token header on mutating API calls"
```

> **Note for later phases:** the `LoginView`, `ConfigView`, `SchedulingConfig`, and `ObservabilityView` components also issue mutating calls. Migrate them to `apiFetch` as they are touched in Phase 2; this task covers the calls in `App.tsx` and the new component.

---

## Task 12: Method-level authorization

**Files:**
- Modify: `hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/LlmConfigController.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/PipelineController.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/SchedulingController.java`
- Modify: `hud-backend/src/main/java/com/hud/news/MacroMetricsController.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/DailyBriefingController.java`

- [ ] **Step 1: Enable method security**

In `hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java`, add the import and class annotation:
```java
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
```
Add `@EnableMethodSecurity` next to the existing `@EnableWebSecurity` on the class.

- [ ] **Step 2: Annotate the admin-only mutating endpoints**

Add `@PreAuthorize("hasRole('ADMIN')")` (import `org.springframework.security.access.prepost.PreAuthorize` in each file) to:
- `LlmConfigController`: `saveConfig`, `deleteConfig`, `toggleActive`, `runModel`.
- `PipelineController`: the `DELETE` flush method.
- `SchedulingController`: the `PUT` update method and the `POST` init/seed method.
- `MacroMetricsController`: `triggerUpdate`, `triggerCorrelation`, `triggerSync`, `triggerPredictions` (the four `POST` trigger methods).
- `DailyBriefingController`: the `POST /trigger` method.

Example (`LlmConfigController.deleteConfig`):
```java
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteConfig(@PathVariable Long id) {
        repository.deleteById(id);
    }
```

- [ ] **Step 3: Verify the context loads and existing controller tests pass**

Run: `mvn test -pl hud-backend -Dtest=LlmConfigControllerTest,PipelineControllerTest,SchedulingControllerTest,MacroMetricsControllerTest,DailyBriefingControllerTest -DexcludedGroups=`
Expected: PASS — these are unit tests that call controller methods directly, so `@PreAuthorize` (enforced by the Spring proxy, not in direct calls) does not change their behavior.

- [ ] **Step 4: Commit**

```bash
git add hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java \
  hud-backend/src/main/java/com/hud/briefing/LlmConfigController.java \
  hud-backend/src/main/java/com/hud/briefing/PipelineController.java \
  hud-backend/src/main/java/com/hud/briefing/SchedulingController.java \
  hud-backend/src/main/java/com/hud/news/MacroMetricsController.java \
  hud-backend/src/main/java/com/hud/briefing/DailyBriefingController.java
git commit -m "feat: add method-level @PreAuthorize to admin endpoints"
```

---

## Task 13: Full API-key masking

**Files:**
- Modify: `hud-backend/src/main/java/com/hud/briefing/LlmConfigController.java`
- Modify: `hud-backend/src/test/java/com/hud/briefing/LlmConfigControllerTest.java`

- [ ] **Step 1: Write the failing test**

In `hud-backend/src/test/java/com/hud/briefing/LlmConfigControllerTest.java`, add a test that a stored key is fully masked on read:
```java
    @Test
    void getAllConfigsFullyMasksApiKey() {
        LlmConfig config = new LlmConfig("Cloud Gemini", LlmProvider.GEMINI, "gemini-2.0-flash", true);
        config.setApiKey("AIzaSyVERYSECRETKEY1234567890");
        when(repository.findAll()).thenReturn(java.util.List.of(config));

        java.util.List<LlmConfig> result = controller.getAllConfigs();

        assertEquals("********", result.get(0).getApiKey());
    }
```
(If the test class uses different field names for the mocked repository/controller, match them; the existing tests in this file show the names. Add the `assertEquals` static import if absent.)

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -pl hud-backend -Dtest=LlmConfigControllerTest#getAllConfigsFullyMasksApiKey -DexcludedGroups=`
Expected: FAIL — the key is masked as `AIza...7890`, not `********`.

- [ ] **Step 3: Mask keys fully**

In `hud-backend/src/main/java/com/hud/briefing/LlmConfigController.java`, replace the body of the `getAllConfigs` `forEach` so any non-blank key becomes `********`:
```java
        configs.forEach(c -> {
            if (c.getApiKey() != null && !c.getApiKey().isBlank()) {
                c.setApiKey("********");
            }
        });
```
Remove the now-unused `MASK_THRESHOLD` constant.

The `saveConfig` "preserve old key when masked" check must still recognise the masked value. Change its condition from `config.getApiKey().contains("...")` to:
```java
            if (config.getApiKey() != null
                    && (config.getApiKey().equals("********") || config.getApiKey().isBlank())) {
                config.setApiKey(existing.getApiKey());
            }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -pl hud-backend -Dtest=LlmConfigControllerTest -DexcludedGroups=`
Expected: PASS (the new test and all existing ones).

- [ ] **Step 5: Commit**

```bash
git add hud-backend/src/main/java/com/hud/briefing/LlmConfigController.java \
  hud-backend/src/test/java/com/hud/briefing/LlmConfigControllerTest.java
git commit -m "fix: fully mask LLM API keys on read"
```

---

## Task 14: CI-safe test gating

**Files:**
- Modify: `hud-backend/pom.xml` (Surefire config)
- Modify: `pom.xml` (root — `integration` profile)
- Modify: external-dependent test files (add `@Tag("integration")`)

- [ ] **Step 1: Audit and tag external-dependent tests**

For each test under `hud-backend/src/test/java` that requires a live Ollama server, a Gemini API key, a real browser, or runs for minutes, ensure the class is annotated `@Tag("integration")` (add `import org.junit.jupiter.api.Tag;` if missing). Apply to at least:
`OllamaConnectionSmokeTest`, `GeminiIntegrationTest`, `DjlEngineSmokeTest`, `ModelComparisonTest`, `FullIntelligenceLifecycleE2E`, `ContentExtractionSmokeTest`, `MarketScraperDebugTest`, `IswExtractionDebugTest`, `IswStructuralAnalysisTest`, `UIInspectionTest`, `UiSmokeTest`, `MarkdownRenderingSmokeTest`, `SummarizationPrototypeTest`, `PlaywrightScraperServiceTest`.

For each tagged class, also confirm it is NOT additionally tagged `@Tag("unit")`. `MigrationIntegrationTest` (Task 2) is already tagged.

- [ ] **Step 2: Verify the tagging**

Run:
```bash
grep -rL '@Tag(' hud-backend/src/test/java --include='*.java' | grep -v package-info
```
Expected: only fast, self-contained test classes remain untagged (acceptable) — no external-dependent class appears. Spot-check the list.

- [ ] **Step 3: Configure Surefire to exclude integration tests by default**

In `hud-backend/pom.xml`, inside `<build><plugins>`, add the Surefire plugin (version is managed by the parent):
```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <excludedGroups>${surefire.excludedGroups}</excludedGroups>
                </configuration>
            </plugin>
```
In the root `pom.xml`, add inside `<properties>`:
```xml
        <surefire.excludedGroups>integration</surefire.excludedGroups>
```

- [ ] **Step 4: Add the `integration` profile**

In the root `pom.xml`, after `</build>` and before `</project>`, add:
```xml
    <profiles>
        <profile>
            <id>integration</id>
            <properties>
                <surefire.excludedGroups></surefire.excludedGroups>
            </properties>
        </profile>
    </profiles>
```

- [ ] **Step 5: Verify the default build is green and fast**

Run: `mvn test -pl hud-backend`
Expected: BUILD SUCCESS — only `@Tag("unit")` and untagged fast tests run; no Ollama/Gemini/browser tests execute, no external connection attempts in the log.

- [ ] **Step 6: Verify the integration profile runs everything**

Run (requires Docker for Testcontainers): `mvn test -pl hud-backend -Pintegration -Dtest=MigrationIntegrationTest`
Expected: PASS — confirms the profile clears the exclusion so integration-tagged tests run.

- [ ] **Step 7: Commit**

```bash
git add pom.xml hud-backend/pom.xml hud-backend/src/test/java
git commit -m "test: exclude integration tests by default, add integration profile"
```

---

## Final Verification

- [ ] **Step 1: Full default build**

Run: `mvn clean install`
Expected: BUILD SUCCESS for `hud-frontend` and `hud-backend`; backend runs unit tests only; frontend runs `vitest`.

- [ ] **Step 2: Confirm no regressions in the security surface**

Run: `mvn test -pl hud-backend -Pintegration -Dtest=MigrationIntegrationTest`
Expected: PASS — schema migrates cleanly and validates.

- [ ] **Step 3: Smoke-test the running stack**

Run: `./bin/deploy.sh --build`, then check the application log for the seeded admin password line (`Seeded admin user with a GENERATED password:` or the `HUD_ADMIN_PASSWORD` info line). Log in at `http://localhost:8889`, confirm the forced password-change view appears, set a compliant password, and confirm normal navigation resumes.

- [ ] **Step 4: Update the README**

Document in `README.md`: the `HUD_ADMIN_PASSWORD` environment variable, the forced first-login password change, that `mvn test` runs unit tests only while `mvn test -Pintegration` (or `./bin/test.sh --int`) runs the full suite, and that `bootstrap_*.sql` is regenerated via `harvest_global.py` / `harvest_history.py`. Commit:
```bash
git add README.md
git commit -m "docs: document admin password setup and test profiles"
```

---

## Notes for the Implementer

- All commands assume the repo root `/home/jakefear/source/hud` unless a `cd` is shown.
- `-DexcludedGroups=` is passed on per-task test runs so a single integration test can be run before Task 14 installs the default exclusion; after Task 14 it is still harmless for unit tests.
- The branch is `feat/stabilization-breadth-upgrade`. Do not open a PR until the user asks.
- Phases 2 and 3 (DB-backed sources, map-reduce, deeper crawl, budget) are out of scope here and get their own plans.
