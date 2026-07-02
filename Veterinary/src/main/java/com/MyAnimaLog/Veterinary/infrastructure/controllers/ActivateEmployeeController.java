package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.ActivateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.ActivateEmployeeUseCase;
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
public class ActivateEmployeeController {

    private final ActivateEmployeeUseCase activateEmployeeUseCase;

    @PatchMapping("/{employeeId}/activate")
    public ResponseEntity<ActivateEmployeeResponse> activate(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(activateEmployeeUseCase.activate(employeeId));
    }
}
