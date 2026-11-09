package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.PreviewVeterinaryLinkRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.PreviewVeterinaryLinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.PreviewVeterinaryLinkUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST} y no {@code GET} aunque sea una lectura: el código va en el cuerpo para que no quede
 * en los logs de acceso del gateway ni de Cloud Run.
 */
@RestController
@RequestMapping("/api/veterinary/links")
@RequiredArgsConstructor
public class PreviewVeterinaryLinkController {

    private final PreviewVeterinaryLinkUseCase previewVeterinaryLinkUseCase;

    @PostMapping("/preview")
    public ResponseEntity<PreviewVeterinaryLinkResponse> preview(
            @RequestBody PreviewVeterinaryLinkRequest request) {
        return ResponseEntity.ok(previewVeterinaryLinkUseCase.preview(request));
    }
}
