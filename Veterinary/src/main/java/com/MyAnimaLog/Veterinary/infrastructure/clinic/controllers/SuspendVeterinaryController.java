package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.SuspendVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.SuspendVeterinaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/admin/veterinaries")
@RequiredArgsConstructor
public class SuspendVeterinaryController {

    private final SuspendVeterinaryUseCase suspendVeterinaryUseCase;

    @PatchMapping("/{veterinaryId}/suspend")
    public ResponseEntity<SuspendVeterinaryResponse> suspend(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(suspendVeterinaryUseCase.suspend(veterinaryId));
    }
}
