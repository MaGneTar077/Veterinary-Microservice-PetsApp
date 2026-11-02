package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.UploadProfessionalDocumentResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.UploadProfessionalDocumentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

@RestController
@RequestMapping("/api/veterinary/me/professional-profile")
@RequiredArgsConstructor
public class UploadProfessionalDocumentController {

    private final UploadProfessionalDocumentUseCase uploadProfessionalDocumentUseCase;

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadProfessionalDocumentResponse> upload(@RequestParam("documentType") String documentType,
                                                                        @RequestParam("file") MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        UploadProfessionalDocumentResponse response =
                uploadProfessionalDocumentUseCase.uploadDocument(documentType, content, file.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
