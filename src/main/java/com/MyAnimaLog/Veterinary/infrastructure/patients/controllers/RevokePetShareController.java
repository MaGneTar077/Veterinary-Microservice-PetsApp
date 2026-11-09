package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.RevokePetShareResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.RevokePetShareUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/me/linked-veterinaries")
@RequiredArgsConstructor
public class RevokePetShareController {

    private final RevokePetShareUseCase revokePetShareUseCase;

    @DeleteMapping("/{veterinaryId}/pets/{petId}")
    public ResponseEntity<RevokePetShareResponse> revoke(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID petId) {
        return ResponseEntity.ok(revokePetShareUseCase.revoke(veterinaryId, petId));
    }
}
