package com.MyAnimaLog.Veterinary.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates {@code /internal/**} requests via the {@code X-Internal-Api-Key} header
 * (CONTRATOS_COMPARTIDOS.md §2) instead of a user JWT. Does nothing for any other path,
 * leaving the JWT resource-server filter to handle authentication as usual.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_SERVICE_AUTHORITY = "INTERNAL_SERVICE";

    private final String internalApiKey;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public InternalApiKeyFilter(@Value("${app.security.internal-api-key}") String internalApiKey,
                                 RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.internalApiKey = internalApiKey;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HEADER);
        if (provided == null || !constantTimeEquals(provided, internalApiKey)) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Missing or invalid " + HEADER));
            return;
        }

        PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(
                "internal-service", null, List.of(new SimpleGrantedAuthority(INTERNAL_SERVICE_AUTHORITY)));
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String provided, String expected) {
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
