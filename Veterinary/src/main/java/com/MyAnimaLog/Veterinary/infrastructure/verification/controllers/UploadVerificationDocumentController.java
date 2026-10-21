package com.MyAnimaLog.Veterinary.infrastructure.verification.controllers;

import com.MyAnimaLog.Veterinary.application.verification.dto.UploadVerificationDocumentResponse;
import com.MyAnimaLog.Veterinary.application.verification.ports.in.UploadVerificationDocumentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class UploadVerificationDocumentController {

    private final UploadVerificationDocumentUseCase uploadVerificationDocumentUseCase;

    @PostMapping(value = "/{veterinaryId}/verification/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadVerificationDocumentResponse> upload(@PathVariable UUID veterinaryId,
                                                                       @RequestParam("documentType") String documentType,
                                                                       @RequestParam("file") MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        UploadVerificationDocumentResponse response = uploadVerificationDocumentUseCase.uploadDocument(veterinaryId, documentType, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
