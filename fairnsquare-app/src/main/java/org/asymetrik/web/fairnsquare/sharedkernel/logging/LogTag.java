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
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogTag {

    /**
     * The tag name to use in the log entry.
     */
    String value();
}
