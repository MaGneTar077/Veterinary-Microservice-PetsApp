package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeActivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.DeActivateVeterinaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class DeActivateVeterinaryController {

    private final DeActivateVeterinaryUseCase deActivateVeterinaryUseCase;

    @PatchMapping("/{veterinaryId}/deactivate")
    public ResponseEntity<DeActivateVeterinaryResponse> deActivate(
            @PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(deActivateVeterinaryUseCase.deActivate(veterinaryId));
    }
}
