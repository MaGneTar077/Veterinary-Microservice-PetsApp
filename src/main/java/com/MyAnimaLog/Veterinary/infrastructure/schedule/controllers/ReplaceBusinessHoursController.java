package com.MyAnimaLog.Veterinary.infrastructure.schedule.controllers;

import com.MyAnimaLog.Veterinary.application.schedule.dto.BusinessHoursResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryRequest;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.ReplaceBusinessHoursUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class ReplaceBusinessHoursController {

    private final ReplaceBusinessHoursUseCase replaceBusinessHoursUseCase;

    /** El cuerpo es la semana completa; una lista vacía deja la clínica sin horario configurado. */
    @PutMapping("/{veterinaryId}/business-hours")
    public ResponseEntity<BusinessHoursResponse> replace(
            @PathVariable UUID veterinaryId,
            @RequestBody(required = false) List<ScheduleEntryRequest> entries) {
        return ResponseEntity.ok(replaceBusinessHoursUseCase.replace(veterinaryId, entries));
    }
}
