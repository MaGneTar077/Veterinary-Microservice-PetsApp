package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.ActivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.ActivateVeterinaryUseCase;
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
public class ActivateVeterinaryController {

    private final ActivateVeterinaryUseCase activateVeterinaryUseCase;

    @PatchMapping("/{veterinaryId}/activate")
    public ResponseEntity<ActivateVeterinaryResponse> activate(
            @PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(activateVeterinaryUseCase.activate(veterinaryId));
    }
}
