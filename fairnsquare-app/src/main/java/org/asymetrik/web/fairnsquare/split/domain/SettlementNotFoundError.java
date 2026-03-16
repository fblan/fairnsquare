package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when no settlement has been computed yet for a split.
 */
public class SettlementNotFoundError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/not-found";
    private static final String TITLE = "Not Found";
    private static final int STATUS = 404;

    public SettlementNotFoundError(String splitId) {
        super(TYPE, TITLE, STATUS, "No settlement computed yet for split: " + splitId);
    }
}
