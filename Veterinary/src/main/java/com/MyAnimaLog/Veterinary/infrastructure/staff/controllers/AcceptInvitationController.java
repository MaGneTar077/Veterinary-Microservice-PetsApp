package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.AcceptInvitationRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.AcceptInvitationResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.AcceptInvitationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary/invitations")
@RequiredArgsConstructor
public class AcceptInvitationController {

    private final AcceptInvitationUseCase acceptInvitationUseCase;

    @PostMapping("/accept")
    public ResponseEntity<AcceptInvitationResponse> accept(@RequestBody AcceptInvitationRequest request) {
        return ResponseEntity.ok(acceptInvitationUseCase.accept(request));
    }
}
