/**
 * App info infrastructure — exposes application version and git commit at startup and via REST.
 */
@Module(name = "appinfo", exports = { "api" })
package org.asymetrik.web.fairnsquare.infrastructure.appinfo;

import org.asymetrik.modular.api.Module;