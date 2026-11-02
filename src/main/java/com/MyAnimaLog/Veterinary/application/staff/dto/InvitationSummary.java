package com.MyAnimaLog.Veterinary.application.staff.dto;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una invitación tal como la ve la clínica. Nunca incluye el token ni su hash.
 *
 * <p>{@code expired} se calcula al momento de responder ({@code expiresAt < now()}): una invitación
 * puede estar {@code PENDING} y vencida a la vez, y en ese caso el frontend debe ofrecer "reenviar",
 * no "esperando respuesta".
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvitationSummary {
    private UUID id;
    private UUID veterinaryId;
    private String email;
    private EmployeeRole role;
    private InvitationStatus status;
    private boolean expired;
    private UUID invitedBy;
    private LocalDateTime expiresAt;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
}
