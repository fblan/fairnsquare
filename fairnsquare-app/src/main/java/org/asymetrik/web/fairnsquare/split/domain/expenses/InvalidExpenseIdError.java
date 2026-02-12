package org.asymetrik.web.fairnsquare.split.domain.expenses;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when an invalid expense ID is provided.
 */
public class InvalidExpenseIdError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/invalid-expense-id";
    private static final String TITLE = "Invalid Expense ID";
    private static final int STATUS = 400;

    public InvalidExpenseIdError(String expenseId) {
        super(TYPE, TITLE, STATUS, "Invalid expense ID format: " + expenseId);
    }
}
