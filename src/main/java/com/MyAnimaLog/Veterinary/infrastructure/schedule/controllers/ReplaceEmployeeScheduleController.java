package com.MyAnimaLog.Veterinary.infrastructure.schedule.controllers;

import com.MyAnimaLog.Veterinary.application.schedule.dto.EmployeeScheduleResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryRequest;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.ReplaceEmployeeScheduleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class ReplaceEmployeeScheduleController {

    private final ReplaceEmployeeScheduleUseCase replaceEmployeeScheduleUseCase;

    /** Una lista vacía borra las filas del empleado y lo devuelve al horario de la clínica. */
    @PutMapping("/{veterinaryId}/employees/{employeeId}/schedule")
    public ResponseEntity<EmployeeScheduleResponse> replace(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID employeeId,
            @RequestBody(required = false) List<ScheduleEntryRequest> entries) {
        return ResponseEntity.ok(replaceEmployeeScheduleUseCase.replace(veterinaryId, employeeId, entries));
    }
}
