package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetPatientAccessResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetPatientAccessUseCase;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.PatientAccessRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.PatientAccessRepositoryPort.AccessRow;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.security.RolePermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Endpoint interno más llamado del ecosistema (Medical lo consulta en cada operación clínica, con
 * caché de 60 s de su lado — CONTRATOS_COMPARTIDOS.md §3). <strong>Nunca</strong> responde 404: una
 * clínica, mascota o usuario inexistentes, o cualquier combinación sin acceso, son todos
 * {@code 200} con {@code hasAccess=false}, para que un consumidor trate cualquier no-2xx como una
 * caída real de este servicio (regla explícita de
 * {@code VETERINARY_REQUERIMIENTOS_DESDE_MEDICAL.md} §3.1/§5).
 *
 * <p>Sin chequeo de autorización de usuario: la llamada ya viene autenticada con
 * {@code X-Internal-Api-Key} (ruta {@code /internal/**}), y el propósito mismo del endpoint es
 * decirle a otro servicio si ESE {@code userId} tiene acceso — no hay un "caller" distinto del
 * sujeto de la consulta.
 */
@Service
@RequiredArgsConstructor
public class GetPatientAccessService implements GetPatientAccessUseCase {

    private static final GetPatientAccessResponse NO_ACCESS = GetPatientAccessResponse.builder()
            .hasAccess(false)
            .licensed(false)
            .permissions(List.of())
            .build();

    private final PatientAccessRepositoryPort accessRepositoryPort;

    @Override
    public GetPatientAccessResponse getAccess(UUID veterinaryId, UUID petId, UUID userId) {
        return accessRepositoryPort.findAccess(veterinaryId, petId, userId)
                .map(this::toResponse)
                .orElse(NO_ACCESS);
    }

    private GetPatientAccessResponse toResponse(AccessRow row) {
        boolean isActiveEmployee = row.employeeId() != null && Boolean.TRUE.equals(row.employeeActive());
        boolean licensed = row.licenseStatus() == LicenseStatus.VERIFIED;
        boolean petGranted = row.petConsentStatus() == ConsentStatus.GRANTED;
        boolean clinicActive = row.veterinaryStatus() == VeterinaryStatus.ACTIVE;

        // Los permisos reflejan rol + licencia + estado de la clínica, sin mirar el consentimiento
        // de ESTA mascota puntual (así lo pide el contrato: "permisos efectivos... para ese rol,
        // licencia y estado"). hasAccess es el gate adicional que sí exige que la mascota esté
        // compartida.
        Set<com.MyAnimaLog.Veterinary.domain.security.Permission> permissions = isActiveEmployee
                ? RolePermissions.resolve(row.role(), licensed, row.veterinaryStatus())
                : Set.of();

        return GetPatientAccessResponse.builder()
                .hasAccess(isActiveEmployee && clinicActive && petGranted)
                .veterinaryStatus(row.veterinaryStatus())
                .employeeId(row.employeeId())
                .role(row.role())
                .licensed(licensed)
                .permissions(List.copyOf(permissions))
                .build();
    }
}
