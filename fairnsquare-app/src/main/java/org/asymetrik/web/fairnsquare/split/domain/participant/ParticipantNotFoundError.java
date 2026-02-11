package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when a participant is not found within a split.
 */
public class ParticipantNotFoundError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/participant-not-found";
    private static final String TITLE = "Participant Not Found";
    private static final int STATUS = 404;

    public ParticipantNotFoundError(String participantId, String splitId) {
        super(TYPE, TITLE, STATUS, "Participant not found: " + participantId + " in split: " + splitId);
    }
}
