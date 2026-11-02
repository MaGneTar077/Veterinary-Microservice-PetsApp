package com.MyAnimaLog.Veterinary.domain.staff.model;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Invitación por correo a formar parte del equipo de una clínica.
 *
 * <p>{@code tokenHash} es el SHA-256 (hex) del token de invitación: el token en claro nunca se
 * guarda ni se loguea, solo viaja en el payload del evento {@code EMPLOYEE_INVITED} hacia el correo
 * del invitado (mismo patrón que los refresh tokens del User service).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class EmployeeInvitation {

    private UUID id;
    private UUID veterinaryId;
    /** Siempre normalizado en minúsculas y sin espacios (ver {@code InviteEmployeeService}). */
    private String email;
    private EmployeeRole role;
    private String tokenHash;
    /** La columna es NOT NULL DEFAULT 'PENDING'; el default también vive aquí (ver CLAUDE.md). */
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;
    private UUID invitedBy;
    private UUID acceptedUserId;
    private LocalDateTime expiresAt;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    /** {@code PENDING} y todavía vigente: el único caso en que se puede aceptar o rechazar. */
    public boolean isUsable(LocalDateTime now) {
        return status == InvitationStatus.PENDING && !isExpired(now);
    }
}
