package org.asymetrik.web.fairnsquare.split.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Expense split modes for distributing costs among participants.
 */
public enum SplitMode {

    /**
     * Distribute expense proportionally based on number of nights stayed.
     */
    BY_NIGHT("BY_NIGHT"),

    /**
     * Distribute expense equally among all participants.
     */
    EQUAL("EQUAL"),

    /**
     * Allow manual specification of each participant's share.
     */
    FREE("FREE");

    private final String value;

    SplitMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
