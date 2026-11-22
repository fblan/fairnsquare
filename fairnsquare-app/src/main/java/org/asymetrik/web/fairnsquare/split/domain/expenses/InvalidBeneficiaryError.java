package org.asymetrik.web.fairnsquare.split.domain.expenses;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when a reimbursement's beneficiary is missing, unknown in the split, or the payer.
 */
public class InvalidBeneficiaryError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/invalid-beneficiary";
    private static final String TITLE = "Invalid Beneficiary";
    private static final int STATUS = 400;

    public InvalidBeneficiaryError(String message) {
        super(TYPE, TITLE, STATUS, message);
    }
}
