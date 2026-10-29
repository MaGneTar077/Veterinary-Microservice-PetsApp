package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.UpdateEmployeeRoleResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.UpdateEmployeeRoleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Alias de compatibilidad. La ruta buena es
 * {@code PATCH /api/veterinary/{veterinaryId}/employees/{employeeId}/role}
 * ({@code UpdateEmployeeRoleNestedController}); esta se mantiene para no romper al frontend y pasa
 * {@code null} como clínica, con lo que el servicio toma la del propio empleado.
 *
 * @deprecated usar la ruta anidada por clínica. Ver FASE_3_PERSONAL.md.
 */
@Deprecated(since = "Fase 3")
@RestController
@RequestMapping("/api/veterinary/employees")
@RequiredArgsConstructor
public class UpdateEmployeeRoleController {

    private final UpdateEmployeeRoleUseCase updateEmployeeRoleUseCase;

    @PatchMapping("/{employeeId}/role")
    public ResponseEntity<UpdateEmployeeRoleResponse> updateRole(
            @PathVariable UUID employeeId,
            @RequestBody UpdateEmployeeRoleRequest request) {
        return ResponseEntity.ok(updateEmployeeRoleUseCase.updateRole(null, employeeId, request));
    }
}
