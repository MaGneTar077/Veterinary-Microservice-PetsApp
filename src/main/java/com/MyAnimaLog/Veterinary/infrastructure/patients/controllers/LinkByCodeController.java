package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkByCodeResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkByCodeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @deprecated Ruta vieja de la Fase 2. Usar {@code POST /api/veterinary/links}. Desde la Fase 4A
 * responde {@code 200} y no {@code 201}: la operación es idempotente, así que un {@code 201} cuando
 * la vinculación ya existía afirmaría que se creó algo que no se creó.
 */
@Deprecated(since = "Fase 4A", forRemoval = true)
@RestController
@RequestMapping("/api/veterinary/link")
@RequiredArgsConstructor
public class LinkByCodeController {

    private final LinkByCodeUseCase linkByCodeUseCase;

    @PostMapping("/code")
    public ResponseEntity<LinkByCodeResponse> linkByCode(@RequestBody LinkByCodeRequest request) {
        return ResponseEntity.ok(linkByCodeUseCase.linkByCode(request));
    }
}