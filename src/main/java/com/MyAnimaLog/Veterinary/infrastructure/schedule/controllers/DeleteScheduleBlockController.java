package com.MyAnimaLog.Veterinary.infrastructure.schedule.controllers;

import com.MyAnimaLog.Veterinary.application.schedule.ports.in.DeleteScheduleBlockUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class DeleteScheduleBlockController {

    private final DeleteScheduleBlockUseCase deleteScheduleBlockUseCase;

    @DeleteMapping("/{veterinaryId}/schedule-blocks/{blockId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID blockId) {
        deleteScheduleBlockUseCase.delete(veterinaryId, blockId);
        return ResponseEntity.noContent().build();
    }
}
