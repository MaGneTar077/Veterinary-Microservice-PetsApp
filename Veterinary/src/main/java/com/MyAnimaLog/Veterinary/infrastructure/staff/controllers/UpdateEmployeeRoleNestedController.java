package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.UpdateEmployeeRoleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Ruta anidada por clínica, la que debe usar el frontend. La vieja
 * ({@code PATCH /api/veterinary/employees/{employeeId}/role}) sigue viva como alias
 * {@code @Deprecated} en {@code UpdateEmployeeRoleController}.
 */
@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class UpdateEmployeeRoleNestedController {

    private final UpdateEmployeeRoleUseCase updateEmployeeRoleUseCase;

    @PatchMapping("/{veterinaryId}/employees/{employeeId}/role")
    public ResponseEntity<UpdateEmployeeRoleResponse> updateRole(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID employeeId,
            @RequestBody UpdateEmployeeRoleRequest request) {
        return ResponseEntity.ok(updateEmployeeRoleUseCase.updateRole(veterinaryId, employeeId, request));
    }
}
