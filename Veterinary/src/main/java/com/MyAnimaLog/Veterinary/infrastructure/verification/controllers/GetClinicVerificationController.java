package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.GetClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.GetClinicVerificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class GetClinicVerificationController {

    private final GetClinicVerificationUseCase getClinicVerificationUseCase;

    @GetMapping("/{veterinaryId}/verification")
    public ResponseEntity<GetClinicVerificationResponse> getVerification(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(getClinicVerificationUseCase.getVerification(veterinaryId));
    }
}
