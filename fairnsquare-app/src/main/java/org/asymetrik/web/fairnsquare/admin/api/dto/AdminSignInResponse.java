package org.asymetrik.web.fairnsquare.admin.api.dto;

/**
 * How admins sign in.
 *
 * @param method
 *            {@code PASSWORD} or {@code OIDC}
 * @param provider
 *            the OIDC provider's name, for its sign-in button; null with the password
 */
public record AdminSignInResponse(String method, String provider) {

    public static AdminSignInResponse password() {
        return new AdminSignInResponse("PASSWORD", null);
    }

    public static AdminSignInResponse oidc(String provider) {
        return new AdminSignInResponse("OIDC", provider);
    }
}
