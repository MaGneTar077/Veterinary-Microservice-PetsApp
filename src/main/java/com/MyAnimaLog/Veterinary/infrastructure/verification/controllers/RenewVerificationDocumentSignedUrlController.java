package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.RenewSignedUrlResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RenewVerificationDocumentSignedUrlUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/admin/verifications")
@RequiredArgsConstructor
public class RenewVerificationDocumentSignedUrlController {

    private final RenewVerificationDocumentSignedUrlUseCase renewVerificationDocumentSignedUrlUseCase;

    @GetMapping("/{requestId}/documents/{documentId}/signed-url")
    public ResponseEntity<RenewSignedUrlResponse> renew(@PathVariable UUID requestId, @PathVariable UUID documentId) {
        return ResponseEntity.ok(renewVerificationDocumentSignedUrlUseCase.renew(requestId, documentId));
    }
}
