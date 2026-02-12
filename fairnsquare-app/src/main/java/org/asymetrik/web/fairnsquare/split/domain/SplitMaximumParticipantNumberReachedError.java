package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

public class SplitMaximumParticipantNumberReachedError extends BaseError {
    public SplitMaximumParticipantNumberReachedError(final Split s) {
        super("https://fairnsquare.app/errors/split-maximum-participant-number-reached",
                "Split Maximum Participant Number Reached", 400,
                String.format(
                        "Split '%s' has reached the maximum number of participants (%d). No more participants can be added.",
                        s.getId(), Split.MAX_PARTICIPANTS));
    }
}
