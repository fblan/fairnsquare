/**
 * Filesystem infrastructure — file storage and tenant path resolution.
 */
@Module(name = "filesystem", exports = { "." }, requires = { "error", "jakarta.cdi", "jakarta.inject", "logging",
        "org.eclipse.microprofile.config", "org.jboss.logging", "quarkus.scheduler.api" })
package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

import org.asymetrik.modular.annotations.Module;
