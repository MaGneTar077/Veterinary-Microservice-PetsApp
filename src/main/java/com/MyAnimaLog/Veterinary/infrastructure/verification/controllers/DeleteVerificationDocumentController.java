package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.ports.in.DeleteVerificationDocumentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class DeleteVerificationDocumentController {

    private final DeleteVerificationDocumentUseCase deleteVerificationDocumentUseCase;

    @DeleteMapping("/{veterinaryId}/verification/documents/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID veterinaryId, @PathVariable UUID documentId) {
        deleteVerificationDocumentUseCase.deleteDocument(veterinaryId, documentId);
        return ResponseEntity.noContent().build();
    }
}
