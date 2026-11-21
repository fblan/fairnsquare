package org.asymetrik.web.fairnsquare.sharedkernel.validation;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/** A request whose fields are wrong: 400, with what is wrong as the detail. */
public class ValidationError extends BaseError {

    public ValidationError(String detail) {
        super("https://fairnsquare.app/errors/validation-error", "Validation Error", 400, detail);
    }
}
