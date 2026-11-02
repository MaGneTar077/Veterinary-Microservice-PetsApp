package com.MyAnimaLog.Veterinary.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationTokensTest {

    @Test
    void generate_shouldReturnUrlSafeTokensOf256Bits() {
        String token = InvitationTokens.generate();

        // 32 bytes en Base64 sin padding = 43 caracteres, alfabeto URL-safe.
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    void generate_shouldNeverRepeatATokenAcrossManyCalls() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            tokens.add(InvitationTokens.generate());
        }

        assertThat(tokens).hasSize(1_000);
    }

    @Test
    void hash_shouldBeDeterministicAndHideTheToken() {
        String token = InvitationTokens.generate();

        String hash = InvitationTokens.hash(token);

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}").doesNotContain(token);
        assertThat(InvitationTokens.hash(token)).isEqualTo(hash);
        assertThat(InvitationTokens.hash(InvitationTokens.generate())).isNotEqualTo(hash);
    }
}
