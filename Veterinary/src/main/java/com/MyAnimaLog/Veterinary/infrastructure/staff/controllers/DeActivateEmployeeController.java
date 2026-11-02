package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.DeActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.DeActivateEmployeeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Alias de compatibilidad. La ruta buena es
 * {@code DELETE /api/veterinary/{veterinaryId}/employees/{employeeId}}
 * ({@code RemoveEmployeeController}).
 *
 * @deprecated usar la ruta anidada por clínica. Ver FASE_3_PERSONAL.md.
 */
@Deprecated(since = "Fase 3")
@RestController
@RequestMapping("/api/veterinary/employees")
@RequiredArgsConstructor
public class DeActivateEmployeeController {

    private final DeActivateEmployeeUseCase deActivateEmployeeUseCase;

    @PatchMapping("/{employeeId}/deactivate")
    public ResponseEntity<DeActivateEmployeeResponse> deActivate(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(deActivateEmployeeUseCase.deActivate(null, employeeId));
    }
}
