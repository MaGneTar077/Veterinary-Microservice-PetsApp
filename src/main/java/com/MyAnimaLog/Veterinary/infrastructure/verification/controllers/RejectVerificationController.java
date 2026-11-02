package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.RejectVerificationRequest;
import com.MyAnimaLog.Veterinary.application.verification.dto.RejectVerificationResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RejectVerificationUseCase;
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
public class RejectVerificationController {

    private final RejectVerificationUseCase rejectVerificationUseCase;

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<RejectVerificationResponse> reject(@PathVariable UUID requestId,
                                                                @RequestBody RejectVerificationRequest request) {
        return ResponseEntity.ok(rejectVerificationUseCase.reject(requestId, request));
    }
}
