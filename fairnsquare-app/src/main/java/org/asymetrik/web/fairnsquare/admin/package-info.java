/**
 * Admin module — administration endpoints, protected by the OIDC provider when one is configured, otherwise by a
 * SHA-256-hashed password token. Exposes split statistics to operators. Not linked from normal navigation.
 */
@Module(name = "admin", exports = {}, requires = { "filesystem", "io.quarkus.security.api", "io.smallrye.config",
        "io.smallrye.mutiny", "jakarta.cdi", "jakarta.inject", "jakarta.ws.rs", "org.eclipse.microprofile.config",
        "quarkus.oidc", "split" })
package org.asymetrik.web.fairnsquare.admin;

import org.asymetrik.modular.annotations.Module;
