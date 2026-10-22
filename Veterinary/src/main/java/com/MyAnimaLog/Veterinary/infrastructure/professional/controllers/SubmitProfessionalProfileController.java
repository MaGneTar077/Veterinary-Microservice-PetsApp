package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.SubmitProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.SubmitProfessionalProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary/me/professional-profile")
@RequiredArgsConstructor
public class SubmitProfessionalProfileController {

    private final SubmitProfessionalProfileUseCase submitProfessionalProfileUseCase;

    @PostMapping("/submit")
    public ResponseEntity<SubmitProfessionalProfileResponse> submit() {
        return ResponseEntity.ok(submitProfessionalProfileUseCase.submit());
    }
}
