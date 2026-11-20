package com.MyAnimaLog.Veterinary.infrastructure.schedule.controllers;

import com.MyAnimaLog.Veterinary.application.schedule.dto.GetScheduleRulesResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.GetScheduleRulesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/internal/veterinaries")
@RequiredArgsConstructor
public class GetScheduleRulesController {

    private final GetScheduleRulesUseCase getScheduleRulesUseCase;

    /**
     * {@code from}/{@code to} son obligatorios; se declaran opcionales para que los valide el caso
     * de uso y su ausencia responda {@code 400 INVALID_SCHEDULE_RANGE} con el formato de error del
     * equipo, en vez del error genérico de Spring.
     */
    @GetMapping("/{veterinaryId}/schedule-rules")
    public ResponseEntity<GetScheduleRulesResponse> get(
            @PathVariable UUID veterinaryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) UUID employeeId) {
        return ResponseEntity.ok(getScheduleRulesUseCase.get(veterinaryId, from, to, employeeId));
    }
}
