package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.UpdateVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdateVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.UpdateVeterinaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class UpdateVeterinaryController {

    private final UpdateVeterinaryUseCase updateVeterinaryUseCase;

    @PatchMapping("/{veterinaryId}")
    public ResponseEntity<UpdateVeterinaryResponse> update(
            @PathVariable UUID veterinaryId,
            @RequestBody UpdateVeterinaryRequest request) {
        return ResponseEntity.ok(updateVeterinaryUseCase.update(veterinaryId, request));
    }
}