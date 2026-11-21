package org.asymetrik.web.fairnsquare.sharedkernel.error.internal;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Converts the application's errors ({@link BaseError}, request validation included) to RFC 9457 Problem Details.
 */
@Provider
public class ProblemDetailMapper implements ExceptionMapper<BaseError> {

    @Override
    public Response toResponse(BaseError e) {
        return Response.status(e.getStatus()).type(MediaType.APPLICATION_JSON)
                .entity(new ProblemDetail(e.getType(), e.getTitle(), e.getStatus(), e.getDetail())).build();
    }

    /**
     * RFC 9457 Problem Details record.
     */
    public record ProblemDetail(String type, String title, int status, String detail) {
    }
}
