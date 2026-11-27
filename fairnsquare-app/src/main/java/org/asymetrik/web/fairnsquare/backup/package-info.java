/**
 * Backup module — encrypted archives of all the splits in an S3 bucket: made every night when splits changed or on
 * demand, listed, and compared with the current splits. Off when no bucket is configured.
 */
@Module(name = "backup", exports = { "." }, requires = { "com.fasterxml.jackson.core", "com.fasterxml.jackson.databind",
        "error", "filesystem", "jakarta.annotation", "jakarta.cdi", "jakarta.inject", "java.base",
        "org.eclipse.microprofile.config", "org.jboss.logging", "quarkus.scheduler.api", "software.amazon.awssdk.auth",
        "software.amazon.awssdk.core", "software.amazon.awssdk.http.urlconnection", "software.amazon.awssdk.regions",
        "software.amazon.awssdk.services.s3" })
package org.asymetrik.web.fairnsquare.backup;

import org.asymetrik.modular.annotations.Module;
