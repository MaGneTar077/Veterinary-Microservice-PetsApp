package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.ResendInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.ResendInvitationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class ResendInvitationController {

    private final ResendInvitationUseCase resendInvitationUseCase;

    @PostMapping("/{veterinaryId}/invitations/{invitationId}/resend")
    public ResponseEntity<ResendInvitationResponse> resend(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID invitationId) {
        return ResponseEntity.ok(resendInvitationUseCase.resend(veterinaryId, invitationId));
    }
}
