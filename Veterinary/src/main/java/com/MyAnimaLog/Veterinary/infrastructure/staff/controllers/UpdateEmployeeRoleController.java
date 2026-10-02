package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.UpdateEmployeeRoleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/employees")
@RequiredArgsConstructor
public class UpdateEmployeeRoleController {

    private final UpdateEmployeeRoleUseCase updateEmployeeRoleUseCase;

    @PatchMapping("/{employeeId}/role")
    public ResponseEntity<UpdateEmployeeRoleResponse> updateRole(
            @PathVariable UUID employeeId,
            @RequestBody UpdateEmployeeRoleRequest request) {
        return ResponseEntity.ok(updateEmployeeRoleUseCase.updateRole(employeeId, request));
    }
}
