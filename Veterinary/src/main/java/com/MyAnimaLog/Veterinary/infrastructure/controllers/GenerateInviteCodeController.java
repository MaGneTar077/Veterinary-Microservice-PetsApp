package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.GenerateInviteCodeRequest;
import com.MyAnimaLog.Veterinary.application.dto.GenerateInviteCodeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.GenerateInviteCodeUseCase;
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
public class GenerateInviteCodeController {

    private final GenerateInviteCodeUseCase generateInviteCodeUseCase;

    @PostMapping("/{veterinaryId}/invite-code")
    public ResponseEntity<GenerateInviteCodeResponse> generateInviteCode(
            @PathVariable UUID veterinaryId) {

        GenerateInviteCodeRequest request = GenerateInviteCodeRequest.builder()
                .veterinaryId(veterinaryId)
                .build();

        return ResponseEntity.ok(generateInviteCodeUseCase.generateInviteCode(request));
    }
}
