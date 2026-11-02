package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationQueueResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.ListAdminVerificationsUseCase;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
import com.MyAnimaLog.Veterinary.domain.verification.enums.VerificationRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary/admin/verifications")
@RequiredArgsConstructor
public class ListAdminVerificationsController {

    private final ListAdminVerificationsUseCase listAdminVerificationsUseCase;

    @GetMapping
    public ResponseEntity<AdminVerificationQueueResponse> list(
            @RequestParam(required = false) VerificationRequestStatus status,
            @RequestParam(required = false) SubjectType subjectType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(listAdminVerificationsUseCase.list(status, subjectType, page, size));
    }
}
