package org.asymetrik.web.fairnsquare.infrastructure.captcha.api.internal;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.asymetrik.web.fairnsquare.infrastructure.captcha.api.CaptchaProtected;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.service.CaptchaService;

/**
 * JAX-RS request filter that enforces CAPTCHA token validation on endpoints annotated with {@link CaptchaProtected}.
 * Aborts with 401 Problem Details if the {@code X-Captcha-Token} header is missing or invalid.
 */
@Provider
@CaptchaProtected
public class CaptchaTokenFilter implements ContainerRequestFilter {

    static final String CAPTCHA_TOKEN_HEADER = "X-Captcha-Token";

    private static final String PROBLEM_TYPE = "https://fairnsquare.app/errors/captcha-required";
    private static final String PROBLEM_TITLE = "CAPTCHA Token Required";
    private static final int STATUS = 401;

    private final CaptchaService captchaService;

    @Inject
    public CaptchaTokenFilter(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String token = requestContext.getHeaderString(CAPTCHA_TOKEN_HEADER);
        if (!captchaService.validateToken(token)) {
            requestContext.abortWith(buildUnauthorizedResponse());
        }
    }

    private Response buildUnauthorizedResponse() {
        String body = """
                {"type":"%s","title":"%s","status":%d,"detail":"A valid CAPTCHA token is required. Please solve the CAPTCHA and retry."}
                """
                .formatted(PROBLEM_TYPE, PROBLEM_TITLE, STATUS).strip();
        return Response.status(STATUS).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
}
