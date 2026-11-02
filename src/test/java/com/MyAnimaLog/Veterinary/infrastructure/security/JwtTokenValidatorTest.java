package com.MyAnimaLog.Veterinary.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenValidatorTest {

    private static final String ISSUER = "myanimalog-user-service";
    private static final String AUDIENCE = "myanimalog-api";

    private final OAuth2TokenValidator<Jwt> validator = JwtTokenValidator.of(ISSUER, AUDIENCE);

    @Test
    void validate_succeeds_whenIssuerAndAudienceMatch() {
        Jwt jwt = jwtWith(ISSUER, List.of(AUDIENCE));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_fails_whenIssuerDoesNotMatch() {
        Jwt jwt = jwtWith("someone-else", List.of(AUDIENCE));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void validate_fails_whenAudienceDoesNotMatch() {
        Jwt jwt = jwtWith(ISSUER, List.of("some-other-api"));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt jwtWith(String issuer, List<String> audience) {
        Instant now = Instant.now();
        return new Jwt(
                "token-value",
                now,
                now.plusSeconds(900),
                Map.of("alg", "RS256"),
                Map.of(
                        "iss", issuer,
                        "aud", audience,
                        "sub", "8f1c6e2a-0000-0000-0000-000000000000"
                )
        );
    }
}
