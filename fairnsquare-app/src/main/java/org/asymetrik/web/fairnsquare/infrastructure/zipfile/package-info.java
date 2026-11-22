/**
 * Zip file infrastructure — serialization and deserialization of data to/from zip archives.
 */
@Module(name = "zipfile", exports = { "." }, requires = { "com.fasterxml.jackson.databind",
        "com.fasterxml.jackson.datatype.jsr310", "jakarta.cdi" })
package org.asymetrik.web.fairnsquare.infrastructure.zipfile;

import org.asymetrik.modular.annotations.Module;
