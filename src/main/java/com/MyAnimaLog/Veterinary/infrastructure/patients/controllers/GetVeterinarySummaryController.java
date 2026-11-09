package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinarySummaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetVeterinarySummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/veterinaries")
@RequiredArgsConstructor
public class GetVeterinarySummaryController {

    private final GetVeterinarySummaryUseCase getVeterinarySummaryUseCase;

    @GetMapping("/{veterinaryId}/summary")
    public ResponseEntity<GetVeterinarySummaryResponse> getSummary(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(getVeterinarySummaryUseCase.getSummary(veterinaryId));
    }
}
