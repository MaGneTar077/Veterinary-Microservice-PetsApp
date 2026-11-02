package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.RequestVerificationCorrectionRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.RequestVerificationCorrectionResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RequestVerificationCorrectionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/admin/verifications")
@RequiredArgsConstructor
public class RequestVerificationCorrectionController {

    private final RequestVerificationCorrectionUseCase requestVerificationCorrectionUseCase;

    @PostMapping("/{requestId}/request-correction")
    public ResponseEntity<RequestVerificationCorrectionResponse> requestCorrection(
            @PathVariable UUID requestId, @RequestBody RequestVerificationCorrectionRequest request) {
        return ResponseEntity.ok(requestVerificationCorrectionUseCase.requestCorrection(requestId, request));
    }
}
