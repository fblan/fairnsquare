package org.asymetrik.web.fairnsquare.admin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * JAX-RS name binding annotation that marks endpoints requiring a valid admin token in the {@code X-Admin-Token}
 * request header.
 */
@NameBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminProtected {
}
