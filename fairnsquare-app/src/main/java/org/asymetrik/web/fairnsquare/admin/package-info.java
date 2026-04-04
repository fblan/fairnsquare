/**
 * Admin module — password-protected administration endpoint. Exposes split statistics to operators via a SHA-256-hashed
 * token. Not linked from normal navigation.
 */
@Module(name = "admin", exports = {})
package org.asymetrik.web.fairnsquare.admin;

import org.asymetrik.modular.api.Module;
