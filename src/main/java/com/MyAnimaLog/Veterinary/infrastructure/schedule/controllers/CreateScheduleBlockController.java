package com.MyAnimaLog.Veterinary.infrastructure.schedule.controllers;

import com.MyAnimaLog.Veterinary.application.schedule.dto.CreateScheduleBlockRequest;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleBlockResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.CreateScheduleBlockUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class CreateScheduleBlockController {

    private final CreateScheduleBlockUseCase createScheduleBlockUseCase;

    @PostMapping("/{veterinaryId}/schedule-blocks")
    public ResponseEntity<ScheduleBlockResponse> create(
            @PathVariable UUID veterinaryId,
            @RequestBody CreateScheduleBlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createScheduleBlockUseCase.create(veterinaryId, request));
    }
}
