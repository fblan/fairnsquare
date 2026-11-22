/**
 * Error types for the FairNSquare application. Contains base error classes that module-specific errors extend.
 */
@Module(name = "error", exports = { "." }, requires = { "jakarta.validation", "resteasy.reactive",
        "resteasy.reactive.common" })
package org.asymetrik.web.fairnsquare.sharedkernel.error;

import org.asymetrik.modular.annotations.Module;
