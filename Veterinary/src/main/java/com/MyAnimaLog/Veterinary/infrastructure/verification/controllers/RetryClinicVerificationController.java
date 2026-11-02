package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.RetryClinicVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RetryClinicVerificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class RetryClinicVerificationController {

    private final RetryClinicVerificationUseCase retryClinicVerificationUseCase;

    @PostMapping("/{veterinaryId}/verification/retry")
    public ResponseEntity<RetryClinicVerificationResponse> retry(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(retryClinicVerificationUseCase.retry(veterinaryId));
    }
}
