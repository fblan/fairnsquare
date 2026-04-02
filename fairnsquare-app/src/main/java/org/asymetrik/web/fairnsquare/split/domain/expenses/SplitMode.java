package org.asymetrik.web.fairnsquare.split.domain.expenses;

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
     * Distribute expense proportionally based on participant share.
     */
    BY_SHARE("BY_SHARE"),

    /**
     * Allow manual specification of each participant's share.
     */
    FREE("FREE"),

    /**
     * Distribute expense proportionally based on number of nights stayed, but only for a selected subset of
     * participants (e.g. van guests who do not participate in house rent).
     */
    BY_NIGHT_CUSTOM("BY_NIGHT_CUSTOM");

    private final String value;

    SplitMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
