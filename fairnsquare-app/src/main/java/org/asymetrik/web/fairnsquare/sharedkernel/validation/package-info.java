/**
 * Validation of the REST requests: {@link org.asymetrik.web.fairnsquare.sharedkernel.validation.Violations}, which
 * replaces Bean Validation (vidocq has none).
 */
@Module(name = "validation", exports = { "." })
package org.asymetrik.web.fairnsquare.sharedkernel.validation;

import org.asymetrik.modular.api.Module;
