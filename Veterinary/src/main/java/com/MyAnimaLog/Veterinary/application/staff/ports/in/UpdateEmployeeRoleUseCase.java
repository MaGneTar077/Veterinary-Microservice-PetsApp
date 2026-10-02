package com.MyAnimaLog.Veterinary.application.staff.ports.in;

import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleResponse;

import java.util.UUID;

public interface UpdateEmployeeRoleUseCase {
    UpdateEmployeeRoleResponse updateRole(UUID employeeId,  UpdateEmployeeRoleRequest request);
}
