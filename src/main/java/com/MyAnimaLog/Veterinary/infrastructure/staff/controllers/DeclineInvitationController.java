package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.DeclineInvitationRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.DeclineInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.DeclineInvitationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary/invitations")
@RequiredArgsConstructor
public class DeclineInvitationController {

    private final DeclineInvitationUseCase declineInvitationUseCase;

    @PostMapping("/decline")
    public ResponseEntity<DeclineInvitationResponse> decline(@RequestBody DeclineInvitationRequest request) {
        return ResponseEntity.ok(declineInvitationUseCase.decline(request));
    }
}
