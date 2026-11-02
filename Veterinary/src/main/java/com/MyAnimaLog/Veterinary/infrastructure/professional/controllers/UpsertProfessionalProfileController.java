package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.ProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.dto.UpsertProfessionalProfileRequest;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.UpsertProfessionalProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary/me/professional-profile")
@RequiredArgsConstructor
public class UpsertProfessionalProfileController {

    private final UpsertProfessionalProfileUseCase upsertProfessionalProfileUseCase;

    @PutMapping
    public ResponseEntity<ProfessionalProfileResponse> upsert(@RequestBody UpsertProfessionalProfileRequest request) {
        return ResponseEntity.ok(upsertProfessionalProfileUseCase.upsert(request));
    }
}
