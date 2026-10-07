package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.DeActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.DeActivateEmployeeUseCase;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.CannotModifySelfException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.LastAdminException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeActivateEmployeeService implements DeActivateEmployeeUseCase {

    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final VeterinaryAuthorizationService authorizationService;


    @Override
    public DeActivateEmployeeResponse deActivate(UUID employeeId) {
        VeterinaryEmployee employee = employeeRepositoryPort.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        ClinicContext callerContext = authorizationService.require(employee.getVeterinaryId(), Permission.STAFF_MANAGE);

        VeterinaryEmployee actingEmployee = employeeRepositoryPort.findById(callerContext.employeeId())
                .orElseThrow(EmployeeNotFoundException::new);

        if (actingEmployee.getId().equals(employee.getId())) {
            throw new CannotModifySelfException();
        }

        boolean isDeactivatingLastAdmin = employee.getRole() == EmployeeRole.ADMIN
                && Boolean.TRUE.equals(employee.getActive())
                && employeeRepositoryPort.countByVeterinaryIdAndRoleAndActiveTrue(
                        employee.getVeterinaryId(), EmployeeRole.ADMIN) <= 1;
        if (isDeactivatingLastAdmin) {
            throw new LastAdminException();
        }

        VeterinaryEmployee updated = employee.toBuilder()
                .active(false)
                .build();

        VeterinaryEmployee saved = employeeRepositoryPort.save(updated);

        return DeActivateEmployeeResponse.builder()
                .id(saved.getId())
                .veterinaryId(saved.getVeterinaryId())
                .userId(saved.getUserId())
                .role(saved.getRole())
                .active(saved.getActive())
                .build();
    }
}
