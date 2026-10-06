package com.MyAnimaLog.Veterinary.infrastructure.security;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.util.List;

/**
 * Builds the {@code iss}/{@code aud} validator used by the resource server's {@code JwtDecoder},
 * per CONTRATOS_COMPARTIDOS.md §1.1. Kept separate from {@code SecurityConfig} so it can be unit
 * tested without a real {@code JwtDecoder} (which would need network access to the JWKS endpoint).
 */
final class JwtTokenValidator {

    private JwtTokenValidator() {
    }

    static OAuth2TokenValidator<Jwt> of(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(audience));
        return new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator);
    }
}
