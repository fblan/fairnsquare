package org.asymetrik.web.fairnsquare.split.api;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Adds security-relevant HTTP response headers to all split API responses (bound via {@link SplitApi}).
 * <p>
 * Split IDs act as capability credentials: whoever holds the URL has full access to the split. These headers prevent
 * the URL from leaking through shared caches, insecure transport, or the {@code Referer} header.
 */
@Provider
@SplitApi
public class SplitCacheControlFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        responseContext.getHeaders().putSingle("Cache-Control", "private, no-store");
        responseContext.getHeaders().putSingle("Strict-Transport-Security", "max-age=31536000");
        responseContext.getHeaders().putSingle("Referrer-Policy", "no-referrer");
    }
}
