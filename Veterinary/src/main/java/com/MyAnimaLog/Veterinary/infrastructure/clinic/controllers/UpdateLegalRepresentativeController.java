package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateLegalRepresentativeRequest;
import com.MyAnimaLog.Veterinary.application.clinic.dto.UpdateLegalRepresentativeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.UpdateLegalRepresentativeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class UpdateLegalRepresentativeController {

    private final UpdateLegalRepresentativeUseCase updateLegalRepresentativeUseCase;

    @PutMapping("/{veterinaryId}/legal-representative")
    public ResponseEntity<UpdateLegalRepresentativeResponse> updateLegalRepresentative(
            @PathVariable UUID veterinaryId,
            @RequestBody UpdateLegalRepresentativeRequest request) {
        return ResponseEntity.ok(updateLegalRepresentativeUseCase.updateLegalRepresentative(veterinaryId, request));
    }
}
