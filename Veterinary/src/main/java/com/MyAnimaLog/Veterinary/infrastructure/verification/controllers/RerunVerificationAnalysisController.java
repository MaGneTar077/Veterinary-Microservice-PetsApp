package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.RerunVerificationAnalysisResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.RerunVerificationAnalysisUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/admin/verifications")
@RequiredArgsConstructor
public class RerunVerificationAnalysisController {

    private final RerunVerificationAnalysisUseCase rerunVerificationAnalysisUseCase;

    @PostMapping("/{requestId}/rerun-ai")
    public ResponseEntity<RerunVerificationAnalysisResponse> rerun(@PathVariable UUID requestId) {
        return ResponseEntity.ok(rerunVerificationAnalysisUseCase.rerun(requestId));
    }
}
