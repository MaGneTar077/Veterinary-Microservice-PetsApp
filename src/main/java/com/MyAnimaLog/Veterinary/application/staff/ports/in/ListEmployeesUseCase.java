package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.ListEmployeesResponse;

import java.util.UUID;

public interface ListEmployeesUseCase {

    /**
     * @param includeInactive incluir a los empleados retirados (historial); por defecto solo activos
     */
    ListEmployeesResponse list(UUID veterinaryId, boolean includeInactive);
}
