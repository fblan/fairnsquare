package org.asymetrik.web.fairnsquare.admin.api.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.asymetrik.web.fairnsquare.admin.api.AdminProtected;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * JAX-RS request filter that enforces admin token validation on endpoints annotated with {@link AdminProtected}.
 * <p>
 * The incoming {@code X-Admin-Token} header value is hashed with SHA-256 and compared to the configured
 * {@code admin.password-hash}. Aborts with 401 if the token is missing or does not match. If
 * {@code admin.password-hash} is not configured, aborts with 503 (admin access disabled).
 */
@Provider
@AdminProtected
public class AdminTokenFilter implements ContainerRequestFilter {

    static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final String passwordHash;

    @Inject
    public AdminTokenFilter(@ConfigProperty(name = "admin.password-hash", defaultValue = "") String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (passwordHash == null || passwordHash.isBlank()) {
            requestContext.abortWith(buildResponse(503, "Admin access is not configured."));
            return;
        }

        String token = requestContext.getHeaderString(ADMIN_TOKEN_HEADER);
        if (token == null || !sha256Hex(token).equalsIgnoreCase(passwordHash)) {
            requestContext.abortWith(buildResponse(401, "Invalid or missing admin token."));
        }
    }

    private Response buildResponse(int status, String detail) {
        String body = ("{\"status\":%d,\"detail\":\"%s\"}").formatted(status, detail);
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
