package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.LinkToVeterinaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responde {@code 200} y no {@code 201} también cuando crea la vinculación: la operación es
 * idempotente y el mismo POST repetido da el mismo {@code 200}, así que un código de estado que
 * distinga "creado" de "ya estaba" solo haría pensar que hay que tratarlos distinto. La respuesta
 * lo dice con {@code alreadyLinked}.
 */
@RestController
@RequestMapping("/api/veterinary/links")
@RequiredArgsConstructor
public class LinkToVeterinaryController {

    private final LinkToVeterinaryUseCase linkToVeterinaryUseCase;

    @PostMapping
    public ResponseEntity<LinkToVeterinaryResponse> link(@RequestBody LinkToVeterinaryRequest request) {
        return ResponseEntity.ok(linkToVeterinaryUseCase.link(request));
    }
}
