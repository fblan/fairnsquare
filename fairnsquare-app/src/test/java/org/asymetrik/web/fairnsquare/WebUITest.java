package org.asymetrik.web.fairnsquare;

import io.quarkiverse.quinoa.testing.QuinoaTestProfiles;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import org.junit.jupiter.api.Test;

/**
 * Triggers Quinoa to run frontend tests (vitest) during the Maven test phase. The
 * {@link QuinoaTestProfiles.EnableAndRunTests} profile enables Quinoa and sets {@code quarkus.quinoa.run-tests=true},
 * causing {@code npm run test} to execute as part of the Quarkus test lifecycle.
 */
@QuarkusTest
@TestProfile(QuinoaTestProfiles.EnableAndRunTests.class)
class WebUITest {

    @Test
    void runFrontendTests() {
        // No body needed — Quinoa executes the frontend test command
        // automatically as part of the Quarkus test lifecycle.
    }
}
