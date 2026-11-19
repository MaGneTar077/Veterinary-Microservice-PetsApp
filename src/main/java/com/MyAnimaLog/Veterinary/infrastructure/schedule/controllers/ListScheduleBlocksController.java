package com.MyAnimaLog.Veterinary.infrastructure.schedule.controllers;

import com.MyAnimaLog.Veterinary.application.schedule.dto.ListScheduleBlocksResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.ListScheduleBlocksUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class ListScheduleBlocksController {

    private final ListScheduleBlocksUseCase listScheduleBlocksUseCase;

    /**
     * {@code from} y {@code to} son obligatorios, pero se declaran opcionales a propósito: así los
     * valida el caso de uso y su ausencia responde con el formato de error del equipo
     * (400 INVALID_SCHEDULE_RANGE) en vez del error genérico de Spring por parámetro faltante.
     */
    @GetMapping("/{veterinaryId}/schedule-blocks")
    public ResponseEntity<ListScheduleBlocksResponse> list(
            @PathVariable UUID veterinaryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return ResponseEntity.ok(listScheduleBlocksUseCase.list(veterinaryId, from, to));
    }
}
