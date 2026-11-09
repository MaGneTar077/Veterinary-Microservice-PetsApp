package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.UnlinkUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @deprecated Ruta vieja de la Fase 2. El lado dueño usa
 * {@code DELETE /api/veterinary/me/linked-veterinaries/{veterinaryId}} y el lado clínica
 * {@code DELETE /api/veterinary/{veterinaryId}/clients/{userId}} (Fase 4A, bloque d). Misma
 * lógica por debajo ({@link UnlinkUseCase}), solo cambia la ruta.
 */
@Deprecated(since = "Fase 4A", forRemoval = true)
@RestController
@RequestMapping("/api/veterinary/link")
@RequiredArgsConstructor
public class UnlinkController {

    private final UnlinkUseCase unlinkUseCase;

    @DeleteMapping("/{veterinaryId}/user/{userId}")
    public ResponseEntity<UnlinkResponse> unlink(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(unlinkUseCase.unlink(userId, veterinaryId));
    }
}
