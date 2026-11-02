package com.MyAnimaLog.Veterinary.application.staff.dto;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Invitaciones pendientes y vigentes dirigidas al correo del token. No incluye el token de cada
 * invitación: desde esta pantalla no se puede aceptar, solo ver que existen — aceptar requiere el
 * enlace que llegó al correo. Así la lista no se convierte en una forma de aceptar invitaciones sin
 * tener acceso al buzón.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetMyInvitationsResponse {

    private List<MyInvitation> invitations;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyInvitation {
        private UUID invitationId;
        private UUID veterinaryId;
        private String veterinaryName;
        private String veterinaryCity;
        private String logoUrl;
        private EmployeeRole role;
        private LocalDateTime expiresAt;
    }
}
