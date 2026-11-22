/**
 * CAPTCHA infrastructure — stateless human-verification challenges. Contains image generation, AES-GCM encrypted
 * challenge tokens, HMAC-SHA256 proof tokens, the REST API, and the JAX-RS request filter.
 */
@Module(name = "captcha", exports = { "org.asymetrik.web.fairnsquare.infrastructure.captcha.api" }, requires = {
        "com.fasterxml.jackson.annotation", "error", "jakarta.cdi", "jakarta.inject", "jakarta.validation",
        "jakarta.ws.rs", "java.base", "java.desktop", "jnanoid", "org.eclipse.microprofile.config",
        "org.eclipse.microprofile.openapi" })
package org.asymetrik.web.fairnsquare.infrastructure.captcha;

import org.asymetrik.modular.annotations.Module;
