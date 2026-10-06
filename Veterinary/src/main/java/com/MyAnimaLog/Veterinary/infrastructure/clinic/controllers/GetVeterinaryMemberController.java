package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetVeterinaryMemberResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetVeterinaryMemberUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/veterinaries")
@RequiredArgsConstructor
public class GetVeterinaryMemberController {

    private final GetVeterinaryMemberUseCase getVeterinaryMemberUseCase;

    @GetMapping("/{veterinaryId}/members/{userId}")
    public ResponseEntity<GetVeterinaryMemberResponse> getMember(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(getVeterinaryMemberUseCase.getMember(veterinaryId, userId));
    }
}
