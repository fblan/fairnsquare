package org.asymetrik.web.fairnsquare.sharedkernel.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * CDI interceptor binding that enables automatic logging of method invocations. When placed on a class, all public
 * methods are logged. When placed on a method, only that method is logged.
 * <p>
 * Logs at INFO on success and ERROR on exception, including method name, tagged parameters (see {@link LogTag}),
 * result, and elapsed time.
 */
@InterceptorBinding
@Inherited
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
}
