package org.asymetrik.web.fairnsquare.infrastructure.captcha.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * JAX-RS name binding annotation that marks endpoints requiring a valid CAPTCHA token in the {@code X-Captcha-Token}
 * request header.
 */
@NameBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CaptchaProtected {
}
