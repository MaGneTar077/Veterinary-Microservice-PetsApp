package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.SharePetsWithVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.SharePetsWithVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.SharePetsWithVeterinaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/me/linked-veterinaries")
@RequiredArgsConstructor
public class SharePetsWithVeterinaryController {

    private final SharePetsWithVeterinaryUseCase sharePetsWithVeterinaryUseCase;

    @PostMapping("/{veterinaryId}/pets")
    public ResponseEntity<SharePetsWithVeterinaryResponse> share(
            @PathVariable UUID veterinaryId,
            @RequestBody SharePetsWithVeterinaryRequest request) {
        return ResponseEntity.ok(sharePetsWithVeterinaryUseCase.share(veterinaryId, request));
    }
}
