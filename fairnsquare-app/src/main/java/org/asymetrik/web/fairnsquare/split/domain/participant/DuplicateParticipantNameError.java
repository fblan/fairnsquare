package org.asymetrik.web.fairnsquare.split.domain.participant;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;
import org.asymetrik.web.fairnsquare.split.domain.Split;

public class DuplicateParticipantNameError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/duplicate-participant-name";
    private static final String TITLE = "Duplicate Participant Name";
    private static final int STATUS = 400;

    public DuplicateParticipantNameError(Participant participant, Split split) {
        super(TYPE, TITLE, STATUS, String.format(
                "A participant with the name '%s' already exists in split '%s'. Participant names must be unique.",
                participant.name(), split.getId().value()));
    }
}
