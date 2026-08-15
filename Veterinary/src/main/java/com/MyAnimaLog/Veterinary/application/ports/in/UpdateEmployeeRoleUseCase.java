package com.MyAnimaLog.Veterinary.application.ports.in;

import com.MyAnimaLog.Veterinary.application.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdateEmployeeRoleResponse;

import java.util.UUID;

public interface UpdateEmployeeRoleUseCase {
    UpdateEmployeeRoleResponse updateRole(UUID employeeId,  UpdateEmployeeRoleRequest request);
}
