package org.asymetrik.web.fairnsquare.sharedkernel.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that a {@code double} value has no fractional part.
 */
public class WholeNumberValidator implements ConstraintValidator<WholeNumber, Double> {

    @Override
    public boolean isValid(Double value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value % 1 == 0;
    }
}
