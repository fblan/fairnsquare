package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when an invalid split ID is provided.
 */
public class InvalidSplitIdError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/invalid-split-id";
    private static final String TITLE = "Invalid Split ID";
    private static final int STATUS = 400;

    public InvalidSplitIdError(String splitId) {
        super(TYPE, TITLE, STATUS, "Invalid split ID format: " + splitId);
    }
}
