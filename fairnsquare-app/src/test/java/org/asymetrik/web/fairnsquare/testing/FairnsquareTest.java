package org.asymetrik.web.fairnsquare.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Runs a test class against the application, booted in-process by {@link FairnsquareTestRuntime}: the class's
 * {@code @Inject} fields are filled from the application's CDI container, and RestAssured points at its HTTP port.
 * <p>
 * Test classes share one application and one temporary data directory, as with {@code @QuarkusTest}: each test creates
 * splits with their own ids, so none needs cleaning up after. A class that sets {@link #maxFileCount} gets an
 * application of its own, with that limit and an empty data directory (the application restarts before it, and again
 * for the next class).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(FairnsquareTestRuntime.class)
public @interface FairnsquareTest {

    /** {@code fairnsquare.storage.max-file-count}; 0 keeps the tests' default. */
    int maxFileCount() default 0;
}
