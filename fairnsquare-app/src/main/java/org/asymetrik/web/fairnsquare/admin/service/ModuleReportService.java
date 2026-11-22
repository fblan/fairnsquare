package org.asymetrik.web.fairnsquare.admin.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * modular's module report of the running build: one self-contained HTML page, which the build writes into the classes
 * ({@code admin-report/index.html}, see the pom), outside the public {@code META-INF/resources}.
 */
@ApplicationScoped
public class ModuleReportService {

    static final String PAGE = "/admin-report/index.html";

    /**
     * @return the report's page, or empty when this build has none (run from an IDE without Maven's
     *         {@code process-classes} phase)
     */
    public Optional<String> page() {
        try (InputStream page = ModuleReportService.class.getResourceAsStream(PAGE)) {
            return page == null ? Optional.empty()
                    : Optional.of(new String(page.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
