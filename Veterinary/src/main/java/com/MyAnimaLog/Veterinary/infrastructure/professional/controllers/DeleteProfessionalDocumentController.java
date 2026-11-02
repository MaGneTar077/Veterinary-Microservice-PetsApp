package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.ports.in.DeleteProfessionalDocumentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/me/professional-profile")
@RequiredArgsConstructor
public class DeleteProfessionalDocumentController {

    private final DeleteProfessionalDocumentUseCase deleteProfessionalDocumentUseCase;

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID documentId) {
        deleteProfessionalDocumentUseCase.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
