package org.asymetrik.web.fairnsquare.admin.api.internal;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * How admins sign in: with the OIDC provider when one is configured ({@code quarkus.oidc.auth-server-url}, from
 * {@code ADMIN_OIDC_ISSUER}), otherwise with the admin password.
 */
@ApplicationScoped
public class AdminSignIn {

    private final boolean oidc;
    private final Optional<String> group;
    private final String providerName;

    @Inject
    public AdminSignIn(@ConfigProperty(name = "quarkus.oidc.auth-server-url") Optional<String> issuer,
            @ConfigProperty(name = "admin.oidc.group") Optional<String> group,
            @ConfigProperty(name = "admin.oidc.provider-name", defaultValue = "Pocket ID") String providerName) {
        this.oidc = issuer.filter(url -> !url.isBlank()).isPresent();
        this.group = group.filter(name -> !name.isBlank());
        this.providerName = providerName;
    }

    /** Signs in with the admin password. */
    public static AdminSignIn password() {
        return new AdminSignIn(Optional.empty(), Optional.empty(), "");
    }

    /** Signs in with the OIDC provider; with a group, only its members are admins. */
    public static AdminSignIn oidc(String providerName, Optional<String> group) {
        return new AdminSignIn(Optional.of("https://issuer"), group, providerName);
    }

    public boolean oidc() {
        return oidc;
    }

    /** The provider's group whose members are admins; without it, everyone the provider signs in is. */
    public Optional<String> group() {
        return group;
    }

    /** The provider's name, shown on the sign-in button. */
    public String providerName() {
        return providerName;
    }
}
