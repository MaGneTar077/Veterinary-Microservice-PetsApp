package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeleteInviteCodeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.DeleteInviteCodeUseCase;
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
public class DeleteInviteCodeController {

    private final DeleteInviteCodeUseCase deleteInviteCodeUseCase;

    @DeleteMapping("/{veterinaryId}/invite-code")
    public ResponseEntity<DeleteInviteCodeResponse> deleteInviteCode(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(deleteInviteCodeUseCase.deleteInviteCode(veterinaryId));
    }
}
