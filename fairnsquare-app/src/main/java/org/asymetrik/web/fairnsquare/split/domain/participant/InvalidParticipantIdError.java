package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when an invalid participant ID is provided.
 */
public class InvalidParticipantIdError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/invalid-participant-id";
    private static final String TITLE = "Invalid Participant ID";
    private static final int STATUS = 400;

    public InvalidParticipantIdError(String participantId) {
        super(TYPE, TITLE, STATUS, "Invalid participant ID format: " + participantId);
    }
}
