package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.UnlinkUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/link")
@RequiredArgsConstructor
public class UnlinkController {

    private final UnlinkUseCase unlinkUseCase;

    @DeleteMapping("/{veterinaryId}/user/{userId}")
    public ResponseEntity<UnlinkResponse> unlink(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(unlinkUseCase.unlink(userId, veterinaryId));
    }
}
