package com.MyAnimaLog.Veterinary.infrastructure.schedule.controllers;

import com.MyAnimaLog.Veterinary.application.schedule.dto.EmployeeScheduleResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.GetEmployeeScheduleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class GetEmployeeScheduleController {

    private final GetEmployeeScheduleUseCase getEmployeeScheduleUseCase;

    @GetMapping("/{veterinaryId}/employees/{employeeId}/schedule")
    public ResponseEntity<EmployeeScheduleResponse> get(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(getEmployeeScheduleUseCase.get(veterinaryId, employeeId));
    }
}
