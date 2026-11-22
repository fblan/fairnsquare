/**
 * Split domain — the core bounded context for expense sharing. Contains API, domain, service, and persistence layers.
 */
@Module(name = "split", exports = { "." }, requires = { "captcha", "com.fasterxml.jackson.annotation",
        "com.fasterxml.jackson.databind", "com.fasterxml.jackson.datatype.jsr310", "error", "filesystem",
        "io.quarkus.core", "jakarta.cdi", "jakarta.inject", "jakarta.validation", "jakarta.ws.rs", "jnanoid", "logging",
        "org.eclipse.microprofile.openapi", "quarkus.arc", "validation", "zipfile" })
package org.asymetrik.web.fairnsquare.split;

import org.asymetrik.modular.annotations.Module;
