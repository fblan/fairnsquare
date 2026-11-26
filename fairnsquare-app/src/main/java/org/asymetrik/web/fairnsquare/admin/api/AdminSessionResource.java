package org.asymetrik.web.fairnsquare.admin.api;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.asymetrik.web.fairnsquare.admin.api.dto.AdminSignInResponse;
import org.asymetrik.web.fairnsquare.admin.api.internal.AdminSignIn;

import io.quarkus.oidc.OidcSession;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;

/**
 * Signing in to the admin pages: how (password or OIDC provider), and, with the provider, signing in and out.
 */
@Path("/api/admin/session")
public class AdminSessionResource {

    private final AdminSignIn signIn;
    private final OidcSession oidcSession;

    @Inject
    public AdminSessionResource(AdminSignIn signIn, OidcSession oidcSession) {
        this.signIn = signIn;
        this.oidcSession = oidcSession;
    }

    /** How admins sign in, for the admin page to show the password field or the provider's button. */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AdminSignInResponse method() {
        return signIn.oidc() ? AdminSignInResponse.oidc(signIn.providerName()) : AdminSignInResponse.password();
    }

    /**
     * The provider's sign-in. Unauthenticated, the request is redirected to the provider, which redirects back here
     * once the user is signed in; then this sends the browser back to the admin page, with the session cookie.
     */
    @GET
    @Path("/login")
    @Authenticated
    public Response login() {
        if (!signIn.oidc()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.seeOther(URI.create("/admin")).build();
    }

    /** Ends the admin session (removes its cookie). The user stays signed in at the provider. */
    @POST
    @Path("/logout")
    public Uni<Response> logout() {
        if (!signIn.oidc()) {
            return Uni.createFrom().item(Response.noContent().build());
        }
        return oidcSession.logout().replaceWith(() -> Response.noContent().build());
    }
}
