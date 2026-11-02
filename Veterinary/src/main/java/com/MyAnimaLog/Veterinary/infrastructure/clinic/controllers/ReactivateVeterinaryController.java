package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.ReactivateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.ReactivateVeterinaryUseCase;
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
public class ReactivateVeterinaryController {

    private final ReactivateVeterinaryUseCase reactivateVeterinaryUseCase;

    @PatchMapping("/{veterinaryId}/reactivate")
    public ResponseEntity<ReactivateVeterinaryResponse> reactivate(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(reactivateVeterinaryUseCase.reactivate(veterinaryId));
    }
}
