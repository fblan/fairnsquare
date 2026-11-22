/**
 * App info infrastructure — exposes application version and git commit at startup and via REST.
 */
@Module(name = "appinfo", exports = { "api" }, requires = { "io.quarkus.core", "jakarta.cdi", "jakarta.inject",
        "jakarta.ws.rs", "org.eclipse.microprofile.config", "org.eclipse.microprofile.openapi", "org.jboss.logging" })
package org.asymetrik.web.fairnsquare.infrastructure.appinfo;

import org.asymetrik.modular.annotations.Module;
