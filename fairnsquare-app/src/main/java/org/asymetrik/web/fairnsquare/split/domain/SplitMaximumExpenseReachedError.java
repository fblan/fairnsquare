package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

public class SplitMaximumExpenseReachedError extends BaseError {
    public SplitMaximumExpenseReachedError(final Split split) {
        super("https://fairnsquare.app/errors/split-maximum-expense-reached", "Split Maximum Expense Reached", 400,
                String.format(
                        "Split '%s' has reached the maximum number of expenses (%d). No more expenses can be added.",
                        split.getId(), Split.MAX_EXPENSES));
    }
}
