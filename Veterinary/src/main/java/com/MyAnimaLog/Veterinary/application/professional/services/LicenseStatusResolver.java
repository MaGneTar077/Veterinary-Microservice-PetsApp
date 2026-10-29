package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;

import java.util.UUID;

/**
 * Regla única de "¿este usuario está licenciado?": tiene un {@code ProfessionalProfile} con
 * {@code licenseStatus == VERIFIED}. Utilidad estática (no es un puerto ni un servicio), compartida
 * por {@code GetVeterinaryMemberService} —de donde el User service saca el claim
 * {@code vet_licensed}— y por {@code AcceptInvitationService}, para que el valor que ve el invitado
 * al entrar a la clínica no pueda divergir del que termina en su token.
 */
public final class LicenseStatusResolver {

    private LicenseStatusResolver() {
    }

    public static boolean isLicensed(ProfessionalProfileRepositoryPort professionalProfileRepositoryPort,
                                      UUID userId) {
        return professionalProfileRepositoryPort.findByUserId(userId)
                .map(profile -> profile.getLicenseStatus() == LicenseStatus.VERIFIED)
                .orElse(false);
    }
}
