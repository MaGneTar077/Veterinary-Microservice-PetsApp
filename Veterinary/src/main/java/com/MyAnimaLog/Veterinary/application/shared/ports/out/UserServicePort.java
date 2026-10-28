package com.MyAnimaLog.Veterinary.application.shared.ports.out;

import java.util.UUID;

public interface UserServicePort {
    UserInfo getUser(UUID userId);

    /**
     * {@code name} is best-effort: the User service's {@code /internal/users/{id}} contract does not
     * document a name field today, so this is {@code null} until that's confirmed/added on their side —
     * every caller must fall back to another source (e.g. {@code Veterinary.legalRepName}) when it's null.
     */
    record UserInfo(UUID id, String email, Boolean emailVerified, String name) {
    }
}
