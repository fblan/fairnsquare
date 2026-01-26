package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when attempting to remove a participant who has associated expenses.
 */
public class ParticipantHasExpensesError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/participant-has-expenses";
    private static final String TITLE = "Participant Has Expenses";
    private static final int STATUS = 409;

    public ParticipantHasExpensesError() {
        super(TYPE, TITLE, STATUS,
                "Cannot remove participant with associated expenses. Remove or reassign expenses first.");
    }
}
