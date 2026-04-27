package org.asymetrik.web.fairnsquare.sharedkernel.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method parameter to be included as a named tag in the log entry produced by the {@link Log} interceptor.
 * <p>
 * Example usage:
 *
 * <pre>
 * public Optional&lt;Split&gt; getSplit(@LogTag("splitId") String splitId) { ... }
 * </pre>
 *
 * This will produce a log entry containing {@code splitId=<value>}.
 * <p>
 * For parameters that act as capability credentials (e.g. NanoID-based split identifiers), set {@code sensitive=true}.
 * The interceptor will log a truncated SHA-256 hash instead of the raw value.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogTag {

    /**
     * The tag name to use in the log entry.
     */
    String value();

    /**
     * When {@code true}, the interceptor logs a truncated SHA-256 hash of the value instead of the raw value. Use for
     * parameters that function as capability credentials (e.g. split IDs).
     */
    boolean sensitive() default false;
}
