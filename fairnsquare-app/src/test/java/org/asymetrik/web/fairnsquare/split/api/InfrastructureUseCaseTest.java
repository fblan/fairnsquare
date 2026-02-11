package org.asymetrik.web.fairnsquare.split.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.persistence.TenantPathResolver;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Integration tests for infrastructure and configuration validation. Verifies application configuration properties are
 * correctly applied.
 */
@QuarkusTest
class InfrastructureUseCaseTest {

    @Inject
    TenantPathResolver pathResolver;

    @ConfigProperty(name = "fairnsquare.data.path")
    String configuredDataPath;

    @BeforeEach
    void setUp() throws IOException {
        // Clean up any existing test data
        Path defaultTenant = pathResolver.resolveDefaultTenantDirectory();
        if (Files.exists(defaultTenant)) {
            Files.walk(defaultTenant).sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException _) {
                    // Ignore cleanup errors
                }
            });
        }
    }

    /**
     * AC 6: Configurable data path via config property. Verifies that the fairnsquare.data.path config property is used
     * by TenantPathResolver.
     */
    @Test
    void configuredDataPath_isUsedByPathResolver() {
        // Verify config property is injected (proves config mechanism works)
        assertThat(configuredDataPath).as("Config property fairnsquare.data.path should be injected").isNotNull();

        // Verify TenantPathResolver uses the configured path
        Path resolvedPath = pathResolver.resolve("test-split-id");
        assertThat(resolvedPath.toString().startsWith(configuredDataPath))
                .as("Resolved path should start with configured data path: " + configuredDataPath).isTrue();
    }
}
