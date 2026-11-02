package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.GetMyInvitationsResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.GetMyInvitationsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary/me")
@RequiredArgsConstructor
public class GetMyInvitationsController {

    private final GetMyInvitationsUseCase getMyInvitationsUseCase;

    @GetMapping("/invitations")
    public ResponseEntity<GetMyInvitationsResponse> myInvitations() {
        return ResponseEntity.ok(getMyInvitationsUseCase.myInvitations());
    }
}
