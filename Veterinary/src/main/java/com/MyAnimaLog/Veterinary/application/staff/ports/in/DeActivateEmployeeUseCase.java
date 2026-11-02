package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.DeActivateEmployeeResponse;

import java.util.UUID;

/**
 * Retirar a un empleado: {@code active=false} + {@code removed_at}, nunca un DELETE (las historias
 * clínicas que firmó deben seguir siendo atribuibles). Lo exponen
 * {@code DELETE /api/veterinary/{veterinaryId}/employees/{employeeId}} y, como alias
 * {@code @Deprecated}, {@code PATCH /api/veterinary/employees/{employeeId}/deactivate}.
 */
public interface DeActivateEmployeeUseCase {

    /**
     * @param veterinaryIdFromPath clínica de la ruta, o {@code null} si se llamó por la ruta vieja
     *                             (ver {@link UpdateEmployeeRoleUseCase#updateRole})
     */
    DeActivateEmployeeResponse deActivate(UUID veterinaryIdFromPath, UUID employeeId);
}
