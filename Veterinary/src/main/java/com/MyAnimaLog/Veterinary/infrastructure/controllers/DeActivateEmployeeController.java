package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.DeActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.DeActivateEmployeeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/employees")
@RequiredArgsConstructor
public class DeActivateEmployeeController {

    private final DeActivateEmployeeUseCase deActivateEmployeeUseCase;

    @PatchMapping("/{employeeId}/deactivate")
    public ResponseEntity<DeActivateEmployeeResponse> deActivate(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(deActivateEmployeeUseCase.deActivate(employeeId));
    }
}
