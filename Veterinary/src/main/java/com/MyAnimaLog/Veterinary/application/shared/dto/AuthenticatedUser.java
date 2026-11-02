package com.MyAnimaLog.Veterinary.application.shared.dto;

import java.util.Optional;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        boolean emailVerified,
        boolean platformAdmin,
        Optional<ClinicContext> clinic
) {
}
