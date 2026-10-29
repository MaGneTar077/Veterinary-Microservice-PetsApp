package com.MyAnimaLog.Veterinary.application.staff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code seatsUsed} = empleados activos + invitaciones PENDING vigentes después de crear esta
 * invitación. Es el número contra el que se comparará el límite del plan cuando exista
 * (ver TODO(VET-40 / Fase 6) en {@code InviteEmployeeService}); hoy es solo informativo.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InviteEmployeeResponse {
    private InvitationSummary invitation;
    private int seatsUsed;
}
