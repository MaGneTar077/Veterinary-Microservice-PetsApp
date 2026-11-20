package com.MyAnimaLog.Veterinary.application.staff.dto;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Un miembro del equipo para Calendar (CALENDAR_REQUESTS_TO_VETERINARY.md, pedido 4). Sin email ni
 * teléfono: Calendar solo necesita mostrar el nombre y decidir elegibilidad por rol/permisos.
 * {@code name} es best-effort (puede ser {@code null}), nunca inventado ni sustituido por el email.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InternalVeterinaryMemberResponse {
    private UUID employeeId;
    private UUID userId;
    private String name;
    private EmployeeRole role;
    private boolean active;
    private boolean licensed;
    private List<String> permissions;
}
