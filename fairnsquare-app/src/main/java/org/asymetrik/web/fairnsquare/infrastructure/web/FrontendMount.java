package org.asymetrik.web.fairnsquare.infrastructure.web;

import io.vidocq.chappe.api.Handler;
import io.vidocq.chappe.api.StaticFileHandler;
import io.vidocq.runtime.extensions.essentials.chappe.spi.MountConfig;
import io.vidocq.runtime.extensions.essentials.chappe.spi.MountHandlerProvider;

/**
 * The {@code frontend} mount type: the frontend's files, and {@code index.html} for any other path, so that the
 * frontend's own URLs ({@code /splits/...}, {@code /admin}) load the page, which routes them. The REST resources
 * register first, so {@code /api/...} never reaches it.
 * <p>
 * Configured in {@code vidocq.properties} ({@code vidocq.http.mount.ui.*}): the classpath folder and Cache-Control.
 */
public class FrontendMount implements MountHandlerProvider {

    static final String INDEX = "index.html";

    @Override
    public String type() {
        return "frontend";
    }

    @Override
    public Handler create(MountConfig config) {
        StaticFileHandler.Builder files = StaticFileHandler.builder()
                .addClasspath(FrontendMount.class.getClassLoader(), config.property("classpath").orElseThrow())
                .indexFile(INDEX).spaFallback(INDEX);
        config.property("cache-control").ifPresent(files::cacheControl);
        return files.build();
    }
}
