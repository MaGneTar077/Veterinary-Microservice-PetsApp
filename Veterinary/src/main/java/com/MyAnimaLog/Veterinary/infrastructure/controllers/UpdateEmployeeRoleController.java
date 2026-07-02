package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.UpdateEmployeeRoleUseCase;
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
