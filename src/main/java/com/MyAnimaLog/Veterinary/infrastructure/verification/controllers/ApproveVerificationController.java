package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.ApproveVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.ApproveVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.ApproveVerificationUseCase;
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
public class ApproveVerificationController {

    private final ApproveVerificationUseCase approveVerificationUseCase;

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ApproveVerificationResponse> approve(@PathVariable UUID requestId,
                                                                 @RequestBody ApproveVerificationRequest request) {
        return ResponseEntity.ok(approveVerificationUseCase.approve(requestId, request));
    }
}
