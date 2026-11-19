package com.MyAnimaLog.Veterinary.infrastructure.schedule.controllers;

import com.MyAnimaLog.Veterinary.application.schedule.dto.BusinessHoursResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.GetBusinessHoursUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class GetBusinessHoursController {

    private final GetBusinessHoursUseCase getBusinessHoursUseCase;

    @GetMapping("/{veterinaryId}/business-hours")
    public ResponseEntity<BusinessHoursResponse> get(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(getBusinessHoursUseCase.get(veterinaryId));
    }
}
