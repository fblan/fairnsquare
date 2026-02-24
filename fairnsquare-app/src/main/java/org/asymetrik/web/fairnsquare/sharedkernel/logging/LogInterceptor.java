package org.asymetrik.web.fairnsquare.sharedkernel.logging;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * CDI interceptor that logs method invocations annotated with {@link Log}.
 * <p>
 * For each invocation it logs:
 * <ul>
 * <li>The method name</li>
 * <li>Parameters annotated with {@link LogTag} as key=value pairs</li>
 * <li>The result (on success) or exception message (on failure)</li>
 * <li>Elapsed time in milliseconds</li>
 * </ul>
 */
@Log
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class LogInterceptor {

    @AroundInvoke
    public Object logInvocation(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        Logger logger = Logger.getLogger(method.getDeclaringClass());
        String methodName = method.getName();
        String tags = extractTags(method, context.getParameters());

        long start = System.currentTimeMillis();
        try {
            Object result = context.proceed();
            long duration = System.currentTimeMillis() - start;
            logger.infof("method=%s %sresult=%s duration=%dms", methodName, tags, formatResult(result), duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            logger.errorf("method=%s %serror=%s duration=%dms", methodName, tags, e.getMessage(), duration);
            throw e;
        }
    }

    private String extractTags(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        Map<String, Object> tags = new LinkedHashMap<>();

        for (int i = 0; i < parameters.length; i++) {
            LogTag logTag = findLogTag(parameters[i]);
            if (logTag != null) {
                tags.put(logTag.value(), args[i]);
            }
        }

        if (tags.isEmpty()) {
            return "";
        }

        return tags.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(" ")) + " ";
    }

    private Object formatResult(Object result) {
        if (result instanceof Optional<?> opt) {
            return opt.map(Object::toString).orElse("empty");
        }
        return result;
    }

    private LogTag findLogTag(Parameter parameter) {
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation instanceof LogTag logTag) {
                return logTag;
            }
        }
        return null;
    }
}
