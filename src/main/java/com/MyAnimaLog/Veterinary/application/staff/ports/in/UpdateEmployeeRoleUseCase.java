package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleResponse;

import java.util.UUID;

public interface UpdateEmployeeRoleUseCase {

    /**
     * @param veterinaryIdFromPath clínica de la ruta, o {@code null} si se llamó por la ruta vieja
     *                             {@code PATCH /api/veterinary/employees/{employeeId}/role}, que no
     *                             la lleva (alias {@code @Deprecated}); en ese caso se toma la del
     *                             propio empleado
     */
    UpdateEmployeeRoleResponse updateRole(UUID veterinaryIdFromPath, UUID employeeId,
                                           UpdateEmployeeRoleRequest request);
}
