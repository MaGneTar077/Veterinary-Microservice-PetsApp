package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.RevokeInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.RevokeInvitationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class RevokeInvitationController {

    private final RevokeInvitationUseCase revokeInvitationUseCase;

    @DeleteMapping("/{veterinaryId}/invitations/{invitationId}")
    public ResponseEntity<RevokeInvitationResponse> revoke(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID invitationId) {
        return ResponseEntity.ok(revokeInvitationUseCase.revoke(veterinaryId, invitationId));
    }
}
