package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.ProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.GetMyProfessionalProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary/me/professional-profile")
@RequiredArgsConstructor
public class GetMyProfessionalProfileController {

    private final GetMyProfessionalProfileUseCase getMyProfessionalProfileUseCase;

    @GetMapping
    public ResponseEntity<ProfessionalProfileResponse> getMyProfile() {
        return ResponseEntity.ok(getMyProfessionalProfileUseCase.getMyProfile());
    }
}
