package com.MyAnimaLog.Veterinary.infrastructure.security;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @WebMvcTest} only scans controllers, controller advice and a handful of well-known
 * web-related bean types — it does not pick up {@code SecurityConfig}'s plain
 * {@code @Component} collaborators on its own. Add this to any {@code @WebMvcTest} class so
 * the real security filter chain (and therefore {@code .with(jwt())} / the 401/403 JSON
 * format) is exercised instead of failing to load the context.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
        SecurityConfig.class,
        InternalApiKeyFilter.class,
        JwtClaimsConverter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
public @interface ImportSecurityConfig {
}
