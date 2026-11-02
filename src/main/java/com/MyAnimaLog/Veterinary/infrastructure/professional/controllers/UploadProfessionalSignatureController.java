package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.UploadSignatureResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.UploadProfessionalSignatureUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

@RestController
@RequestMapping("/api/veterinary/me/professional-profile")
@RequiredArgsConstructor
public class UploadProfessionalSignatureController {

    private final UploadProfessionalSignatureUseCase uploadProfessionalSignatureUseCase;

    @PutMapping(value = "/signature", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadSignatureResponse> upload(@RequestParam("file") MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return ResponseEntity.ok(uploadProfessionalSignatureUseCase.uploadSignature(content));
    }
}
