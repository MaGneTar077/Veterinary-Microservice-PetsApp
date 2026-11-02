package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.DeActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.DeActivateEmployeeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Retirar a un empleado. Es un {@code DELETE} en la API pero <strong>no borra la fila</strong>:
 * delega en {@code DeActivateEmployeeUseCase}, que marca {@code active=false} + {@code removed_at}
 * para no perder la trazabilidad de las historias clínicas que el empleado firmó.
 */
@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class RemoveEmployeeController {

    private final DeActivateEmployeeUseCase deActivateEmployeeUseCase;

    @DeleteMapping("/{veterinaryId}/employees/{employeeId}")
    public ResponseEntity<DeActivateEmployeeResponse> remove(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(deActivateEmployeeUseCase.deActivate(veterinaryId, employeeId));
    }
}
