package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByUrlResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkByUrlUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @deprecated Ruta vieja de la Fase 2. Usar {@code POST /api/veterinary/links} con el código.
 * Desde la Fase 4A responde {@code 200} y no {@code 201}, por la misma razón que
 * {@code LinkByCodeController}: la operación es idempotente.
 */
@Deprecated(since = "Fase 4A", forRemoval = true)
@RestController
@RequestMapping("/api/veterinary/link")
@RequiredArgsConstructor
public class LinkByUrlController {

    private final LinkByUrlUseCase linkByUrlUseCase;

    @PostMapping("/url")
    public ResponseEntity<LinkByUrlResponse> linkByUrl(@RequestBody LinkByUrlRequest request) {
        return ResponseEntity.ok(linkByUrlUseCase.linkByUrl(request));
    }
}