package org.asymetrik.web.fairnsquare.split.domain.expenses;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when FREE expense shares are invalid (e.g., don't have positive parts, missing participant).
 */
public class InvalidSharesError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/invalid-shares";
    private static final String TITLE = "Invalid Shares";
    private static final int STATUS = 400;

    public InvalidSharesError(String message) {
        super(TYPE, TITLE, STATUS, message);
    }
}
