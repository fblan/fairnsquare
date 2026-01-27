package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when a payer ID does not exist in the split's participants.
 */
public class PayerNotFoundError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/payer-not-found";
    private static final String TITLE = "Payer Not Found";
    private static final int STATUS = 400;

    public PayerNotFoundError(String payerId, String splitId) {
        super(TYPE, TITLE, STATUS, String.format(
                "Payer with ID '%s' not found in split '%s'. The payer must be a participant.", payerId, splitId));
    }
}
