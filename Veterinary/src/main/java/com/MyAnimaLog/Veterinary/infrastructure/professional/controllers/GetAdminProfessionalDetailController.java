package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalDetailResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.GetAdminProfessionalDetailUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/admin/professionals")
@RequiredArgsConstructor
public class GetAdminProfessionalDetailController {

    private final GetAdminProfessionalDetailUseCase getAdminProfessionalDetailUseCase;

    @GetMapping("/{profileId}")
    public ResponseEntity<AdminProfessionalDetailResponse> getDetail(@PathVariable UUID profileId) {
        return ResponseEntity.ok(getAdminProfessionalDetailUseCase.getDetail(profileId));
    }
}
