package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Integration tests for infrastructure and configuration validation. Verifies application configuration properties are
 * correctly applied.
 */
@QuarkusTest
@QuarkusTestResource(value = TempStorageTestResource.class, restrictToAnnotatedClass = true)
class InfrastructureUseCaseTest {

    @Inject
    FileSystemService fileSystemService;

    @ConfigProperty(name = "fairnsquare.data.path")
    String configuredDataPath;

    /**
     * AC 6: Configurable data path via config property. Verifies that the fairnsquare.data.path config property is
     * correctly applied to storage resolution.
     */
    @Test
    void configuredDataPath_isUsedByFileSystemService() {
        // Verify config property is injected (proves config mechanism works)
        assertThat(configuredDataPath).as("Config property fairnsquare.data.path should be injected").isNotNull();

        // Verify FileSystemService resolves paths under the configured data path
        Path resolvedPath = fileSystemService.resolvePath(new Filename("test-split-id.zip"));
        assertThat(resolvedPath.toString().startsWith(configuredDataPath))
                .as("Resolved path should start with configured data path: " + configuredDataPath).isTrue();
    }
}
