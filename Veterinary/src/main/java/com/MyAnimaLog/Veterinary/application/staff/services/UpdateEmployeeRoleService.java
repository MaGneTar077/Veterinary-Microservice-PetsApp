package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.UpdateEmployeeRoleUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyOwnerException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifyPeerAdminException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifySelfException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidEmployeeRoleException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateEmployeeRoleService implements UpdateEmployeeRoleUseCase {

    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;

    @Override
    public UpdateEmployeeRoleResponse updateRole(UUID employeeId, UpdateEmployeeRoleRequest request) {
        if (request.getRole() == null) {
            throw new InvalidEmployeeRoleException();
        }
        if (request.getRole() == EmployeeRole.OWNER) {
            throw new InvalidEmployeeRoleException("Cannot assign the OWNER role through this endpoint");
        }

        VeterinaryEmployee employee = employeeRepositoryPort.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        ClinicContext callerContext = authorizationService.require(employee.getVeterinaryId(), Permission.STAFF_MANAGE);

        // Claims can be up to 60 min stale, so re-read the acting employee's current state
        // from the database instead of trusting the token for this decision-critical check.
        VeterinaryEmployee actingEmployee = employeeRepositoryPort.findById(callerContext.employeeId())
                .orElseThrow(EmployeeNotFoundException::new);

        if (actingEmployee.getId().equals(employee.getId())) {
            throw new CannotModifySelfException();
        }

        if (employee.getRole() == EmployeeRole.OWNER) {
            throw new CannotModifyOwnerException();
        }
        if (actingEmployee.getRole() == EmployeeRole.ADMIN && employee.getRole() == EmployeeRole.ADMIN) {
            throw new CannotModifyPeerAdminException();
        }

        VeterinaryEmployee updated = employee.toBuilder()
                .role(request.getRole())
                .build();

        VeterinaryEmployee saved = employeeRepositoryPort.save(updated);

        return UpdateEmployeeRoleResponse.builder()
                .id(saved.getId())
                .veterinaryId(saved.getVeterinaryId())
                .userId(saved.getUserId())
                .role(saved.getRole())
                .build();
    }
}
