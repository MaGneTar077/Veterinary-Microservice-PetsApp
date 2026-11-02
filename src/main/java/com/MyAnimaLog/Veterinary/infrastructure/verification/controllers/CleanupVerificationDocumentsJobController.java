package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.CleanupVerificationDocumentsResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.CleanupVerificationDocumentsJobUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/jobs")
@RequiredArgsConstructor
public class CleanupVerificationDocumentsJobController {

    private final CleanupVerificationDocumentsJobUseCase cleanupVerificationDocumentsJobUseCase;

    @PostMapping("/verification-documents/cleanup")
    public ResponseEntity<CleanupVerificationDocumentsResponse> cleanup() {
        return ResponseEntity.ok(cleanupVerificationDocumentsJobUseCase.cleanup());
    }
}
