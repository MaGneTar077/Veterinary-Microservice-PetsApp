package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinarySettingsRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateVeterinarySettingsResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.UpdateVeterinarySettingsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class UpdateVeterinarySettingsController {

    private final UpdateVeterinarySettingsUseCase updateVeterinarySettingsUseCase;

    @PatchMapping("/{veterinaryId}/settings")
    public ResponseEntity<UpdateVeterinarySettingsResponse> updateSettings(
            @PathVariable UUID veterinaryId,
            @RequestBody UpdateVeterinarySettingsRequest request) {
        return ResponseEntity.ok(updateVeterinarySettingsUseCase.updateSettings(veterinaryId, request));
    }
}
