package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when an expense is not found within a split.
 */
public class ExpenseNotFoundError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/expense-not-found";
    private static final String TITLE = "Expense Not Found";
    private static final int STATUS = 404;

    public ExpenseNotFoundError(String expenseId, String splitId) {
        super(TYPE, TITLE, STATUS, "Expense not found: " + expenseId + " in split: " + splitId);
    }
}
