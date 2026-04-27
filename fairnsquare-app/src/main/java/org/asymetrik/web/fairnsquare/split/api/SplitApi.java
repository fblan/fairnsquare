package org.asymetrik.web.fairnsquare.split.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * JAX-RS name binding annotation applied to all split endpoints. Used by {@link SplitCacheControlFilter} to add
 * security headers to every response from the split API.
 */
@NameBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SplitApi {
}
