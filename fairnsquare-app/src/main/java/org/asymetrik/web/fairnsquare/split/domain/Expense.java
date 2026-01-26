package org.asymetrik.web.fairnsquare.split.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Expense entity. Minimal implementation for Story 3.3 expense constraint check. Full implementation in Epic 4 (Expense
 * Tracking).
 */
public class Expense {

    private final Participant.Id payerId;

    @JsonCreator
    public Expense(@JsonProperty("payerId") Participant.Id payerId) {
        this.payerId = payerId;
    }

    public Participant.Id getPayerId() {
        return payerId;
    }
}
