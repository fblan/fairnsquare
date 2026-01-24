package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when a split is not found.
 */
public class SplitNotFoundError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/not-found";
    private static final String TITLE = "Not Found";
    private static final int STATUS = 404;

    public SplitNotFoundError(String splitId) {
        super(TYPE, TITLE, STATUS, "Split not found: " + splitId);
    }
}
