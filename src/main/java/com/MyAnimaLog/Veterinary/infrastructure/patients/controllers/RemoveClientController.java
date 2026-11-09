package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.RemoveClientUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class RemoveClientController {

    private final RemoveClientUseCase removeClientUseCase;

    @DeleteMapping("/{veterinaryId}/clients/{userId}")
    public ResponseEntity<UnlinkResponse> remove(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(removeClientUseCase.remove(veterinaryId, userId));
    }
}
