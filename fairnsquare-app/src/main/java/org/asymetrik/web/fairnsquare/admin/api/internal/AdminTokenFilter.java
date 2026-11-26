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
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import org.asymetrik.web.fairnsquare.admin.api.AdminProtected;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * JAX-RS request filter that lets only admins reach the endpoints annotated with {@link AdminProtected}.
 * <p>
 * With an OIDC provider ({@link AdminSignIn#oidc()}), the request must come from a user the provider signed in (the
 * session cookie), else 401; when an admin group is configured, the user must be in it, else 403. The password is then
 * ignored.
 * <p>
 * Otherwise, the incoming {@code X-Admin-Token} header value is hashed with SHA-256 and compared to the configured
 * {@code admin.password-hash}. Aborts with 401 if the token is missing or does not match. If
 * {@code admin.password-hash} is not configured, aborts with 503 (admin access disabled).
 */
@Provider
@AdminProtected
public class AdminTokenFilter implements ContainerRequestFilter {

    public static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final String passwordHash;
    private final AdminSignIn signIn;

    @Inject
    public AdminTokenFilter(@ConfigProperty(name = "admin.password-hash", defaultValue = "") String passwordHash,
            AdminSignIn signIn) {
        this.passwordHash = passwordHash;
        this.signIn = signIn;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (signIn.oidc()) {
            checkSignedIn(requestContext);
            return;
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            requestContext.abortWith(buildResponse(503, "Admin access is not configured."));
            return;
        }

        String token = requestContext.getHeaderString(ADMIN_TOKEN_HEADER);
        if (token == null || !constantTimeHashEquals(sha256Bytes(token), passwordHash)) {
            requestContext.abortWith(buildResponse(401, "Invalid or missing admin token."));
        }
    }

    private void checkSignedIn(ContainerRequestContext requestContext) {
        SecurityContext security = requestContext.getSecurityContext();
        if (security == null || security.getUserPrincipal() == null) {
            requestContext.abortWith(buildResponse(401, "Not signed in."));
        } else if (signIn.group().filter(group -> !security.isUserInRole(group)).isPresent()) {
            requestContext.abortWith(buildResponse(403, "This account is not an admin."));
        }
    }

    private Response buildResponse(int status, String detail) {
        String body = ("{\"status\":%d,\"detail\":\"%s\"}").formatted(status, detail);
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    private static byte[] sha256Bytes(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    // Constant-time comparison: parse the stored hex hash to bytes and compare with MessageDigest.isEqual
    // to prevent timing oracles on the password hash. Lowercasing the stored hash normalises any case variation
    // in the configured value without accepting mixed-case via equalsIgnoreCase (which was also timing-leaky).
    private static boolean constantTimeHashEquals(byte[] computed, String storedHex) {
        try {
            byte[] stored = HexFormat.of().parseHex(storedHex.toLowerCase());
            return MessageDigest.isEqual(computed, stored);
        } catch (Exception e) {
            return false;
        }
    }
}
