package org.asymetrik.web.fairnsquare.sharedkernel.error.internal;

import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Global exception mapper that converts exceptions to RFC 9457 Problem Details format.
 */
public class ProblemDetailMapper {

    private static final String ERROR_BASE_URI = "https://fairnsquare.app/errors/";

    /**
     * Maps Bean Validation constraint violations to Problem Details response.
     */
    @ServerExceptionMapper
    public RestResponse<ProblemDetail> mapConstraintViolation(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream().map(violation -> violation.getMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problem = new ProblemDetail(ERROR_BASE_URI + "validation-error", "Validation Error", 400, detail);

        return RestResponse.status(RestResponse.Status.BAD_REQUEST, problem);
    }

    /**
     * Maps BaseError subclasses to Problem Details response.
     */
    @ServerExceptionMapper
    public RestResponse<ProblemDetail> mapBaseError(BaseError e) {
        ProblemDetail problem = new ProblemDetail(e.getType(), e.getTitle(), e.getStatus(), e.getDetail());

        return RestResponse.ResponseBuilder.<ProblemDetail> create(e.getStatus()).entity(problem).build();
    }

    /**
     * RFC 9457 Problem Details record.
     */
    public record ProblemDetail(String type, String title, int status, String detail) {
    }
}
