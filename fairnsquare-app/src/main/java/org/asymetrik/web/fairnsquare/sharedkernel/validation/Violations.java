package org.asymetrik.web.fairnsquare.sharedkernel.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Checks a request's fields and collects what is wrong with them, then {@link #orThrow()} refuses the request with a
 * 400 Problem Details whose detail is every message, joined by {@code "; "}, as Bean Validation did.
 * <p>
 * A {@code null} value passes every check but {@link #notNull} and {@link #notBlank}, as with Bean Validation.
 */
public final class Violations {

    private final List<String> messages = new ArrayList<>();

    private Violations() {
    }

    /** Starts the checks of a request. */
    public static Violations check() {
        return new Violations();
    }

    /** Refuses a missing body, else validates it. */
    public static <T extends Validated> T valid(T request) {
        if (request == null) {
            throw new ValidationError("Request body is required");
        }
        request.validate();
        return request;
    }

    public Violations notNull(Object value, String message) {
        return when(value == null, message);
    }

    public Violations notBlank(String value, String message) {
        return when(value == null || value.isBlank(), message);
    }

    public Violations notEmpty(Collection<?> value, String message) {
        return when(value == null || value.isEmpty(), message);
    }

    public Violations maxLength(String value, int max, String message) {
        return when(value != null && value.length() > max, message);
    }

    /** At least {@code min}, inclusive. */
    public Violations atLeast(BigDecimal value, String min, String message) {
        return when(value != null && value.compareTo(new BigDecimal(min)) < 0, message);
    }

    /** At least {@code min}, inclusive. */
    public Violations atLeast(Number value, double min, String message) {
        return when(value != null && value.doubleValue() < min, message);
    }

    /** At most {@code max}, inclusive. */
    public Violations atMost(Number value, double max, String message) {
        return when(value != null && value.doubleValue() > max, message);
    }

    /** No fractional part. */
    public Violations wholeNumber(Double value, String message) {
        return when(value != null && value % 1 != 0, message);
    }

    /** Adds {@code message} when {@code wrong}. */
    public Violations when(boolean wrong, String message) {
        if (wrong) {
            messages.add(message);
        }
        return this;
    }

    /** Throws {@link ValidationError} if any check failed. */
    public void orThrow() {
        if (!messages.isEmpty()) {
            throw new ValidationError(String.join("; ", messages));
        }
    }

    /** A request that checks itself. */
    public interface Validated {

        /** Throws {@link ValidationError} if a field is wrong. */
        void validate();
    }
}
