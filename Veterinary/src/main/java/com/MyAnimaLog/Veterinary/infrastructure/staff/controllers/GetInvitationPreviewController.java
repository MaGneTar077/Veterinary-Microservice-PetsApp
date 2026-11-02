package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.GetInvitationPreviewRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.GetInvitationPreviewResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.GetInvitationPreviewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Es un {@code POST} y no un {@code GET} a propósito: el token va en el cuerpo porque el gateway y
 * Cloud Run registran la URL completa de cada petición en logs que no controlamos. No modifica nada.
 */
@RestController
@RequestMapping("/api/veterinary/invitations")
@RequiredArgsConstructor
public class GetInvitationPreviewController {

    private final GetInvitationPreviewUseCase getInvitationPreviewUseCase;

    @PostMapping("/preview")
    public ResponseEntity<GetInvitationPreviewResponse> preview(
            @RequestBody GetInvitationPreviewRequest request) {
        return ResponseEntity.ok(getInvitationPreviewUseCase.preview(request));
    }
}
