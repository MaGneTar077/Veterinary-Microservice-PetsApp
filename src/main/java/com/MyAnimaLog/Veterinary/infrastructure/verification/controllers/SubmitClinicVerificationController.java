package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.SubmitClinicVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.SubmitClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.SubmitClinicVerificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class SubmitClinicVerificationController {

    private final SubmitClinicVerificationUseCase submitClinicVerificationUseCase;

    @PostMapping("/{veterinaryId}/verification/submit")
    public ResponseEntity<SubmitClinicVerificationResponse> submit(@PathVariable UUID veterinaryId,
                                                                     @RequestBody SubmitClinicVerificationRequest request) {
        return ResponseEntity.ok(submitClinicVerificationUseCase.submit(veterinaryId, request));
    }
}
