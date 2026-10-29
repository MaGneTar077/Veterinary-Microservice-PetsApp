package com.MyAnimaLog.Veterinary.application.staff.dto;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * {@code licensed} se calcula con la misma regla que alimenta el claim {@code vet_licensed}
 * ({@code LicenseStatusResolver}), pero el valor autoritativo para el token lo sigue resolviendo el
 * User service contra {@code GET /internal/veterinaries/{id}/members/{userId}} al momento de
 * emitirlo. Acá se devuelve solo para que el frontend sepa si debe pedirle al nuevo veterinario que
 * complete su perfil profesional.
 *
 * <p>{@code reactivated} es {@code true} cuando la persona ya había trabajado en esta clínica y se
 * reactivó su registro en vez de crear uno nuevo.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcceptInvitationResponse {
    private UUID employeeId;
    private UUID veterinaryId;
    private String veterinaryName;
    private EmployeeRole role;
    private boolean licensed;
    private boolean reactivated;
}
