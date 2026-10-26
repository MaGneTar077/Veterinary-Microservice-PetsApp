package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationDetailResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.GetAdminVerificationDetailUseCase;
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
public class GetAdminVerificationDetailController {

    private final GetAdminVerificationDetailUseCase getAdminVerificationDetailUseCase;

    @GetMapping("/{requestId}")
    public ResponseEntity<AdminVerificationDetailResponse> getDetail(@PathVariable UUID requestId) {
        return ResponseEntity.ok(getAdminVerificationDetailUseCase.getDetail(requestId));
    }
}
