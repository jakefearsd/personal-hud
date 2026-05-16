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
 *
 * <p>Note: the 'test' profile is intentionally NOT active here — it disables Flyway
 * and switches to H2; this test must run Flyway against real Postgres.
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
