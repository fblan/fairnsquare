package org.asymetrik.web.fairnsquare.sharedkernel.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates that a {@code double} field has no fractional part (i.e. is a whole number). Null values are considered
 * valid — use {@code @NotNull} in combination if null must be rejected.
 */
@Documented
@Constraint(validatedBy = WholeNumberValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface WholeNumber {

    String message() default "Must be a whole number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
