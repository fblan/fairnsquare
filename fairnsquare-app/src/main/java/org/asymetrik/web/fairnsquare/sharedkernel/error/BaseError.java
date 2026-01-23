package org.asymetrik.web.fairnsquare.sharedkernel.error;

/**
 * Base error class for all module-specific errors. Provides structure for RFC 9457 Problem Details responses.
 */
public abstract class BaseError extends RuntimeException {

    private final String type;
    private final String title;
    private final int status;

    protected BaseError(String type, String title, int status, String detail) {
        super(detail);
        this.type = type;
        this.title = title;
        this.status = status;
    }

    protected BaseError(String type, String title, int status, String detail, Throwable cause) {
        super(detail, cause);
        this.type = type;
        this.title = title;
        this.status = status;
    }

    /**
     * URI identifying the problem type. Example: "https://fairnsquare.app/errors/validation-error"
     */
    public String getType() {
        return type;
    }

    /**
     * Short, human-readable summary of the problem type.
     */
    public String getTitle() {
        return title;
    }

    /**
     * HTTP status code for this error.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Human-readable explanation specific to this occurrence.
     */
    public String getDetail() {
        return getMessage();
    }
}
