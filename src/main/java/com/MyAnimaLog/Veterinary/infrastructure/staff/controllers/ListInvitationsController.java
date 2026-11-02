package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.ListInvitationsResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.ListInvitationsUseCase;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class ListInvitationsController {

    private final ListInvitationsUseCase listInvitationsUseCase;

    /**
     * Sin {@code status} devuelve las pendientes, que es la vista por defecto del panel de personal.
     * Para ver el historial hay que pedir cada estado explícitamente
     * ({@code ACCEPTED}/{@code DECLINED}/{@code REVOKED}).
     */
    @GetMapping("/{veterinaryId}/invitations")
    public ResponseEntity<ListInvitationsResponse> list(
            @PathVariable UUID veterinaryId,
            @RequestParam(defaultValue = "PENDING") InvitationStatus status) {
        return ResponseEntity.ok(listInvitationsUseCase.list(veterinaryId, status));
    }
}
