package org.asymetrik.web.fairnsquare.sharedkernel.logging;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Test-only CDI bean annotated with @Log for verifying the LogInterceptor.
 */
@Log
@ApplicationScoped
public class LoggedTestService {

    public String greet(@LogTag("name") String name) {
        return "Hello, " + name;
    }

    public String multiTag(@LogTag("first") String first, @LogTag("second") String second) {
        return first + "-" + second;
    }

    public void noTags(String value) {
        // method with no @LogTag parameters
    }

    public Optional<String> findPresent(@LogTag("id") String id) {
        return Optional.of("found-" + id);
    }

    public Optional<String> findAbsent(@LogTag("id") String id) {
        return Optional.empty();
    }

    public void failing(@LogTag("id") String id) {
        throw new IllegalStateException("test failure for " + id);
    }
}
