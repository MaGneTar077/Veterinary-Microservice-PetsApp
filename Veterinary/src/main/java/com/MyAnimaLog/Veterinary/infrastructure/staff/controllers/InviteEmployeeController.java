package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.InviteEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.InviteEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.InviteEmployeeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class InviteEmployeeController {

    private final InviteEmployeeUseCase inviteEmployeeUseCase;

    @PostMapping("/{veterinaryId}/invitations")
    public ResponseEntity<InviteEmployeeResponse> invite(
            @PathVariable UUID veterinaryId,
            @RequestBody InviteEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inviteEmployeeUseCase.invite(veterinaryId, request));
    }
}
