package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.UnlinkMyVeterinaryUseCase;
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
public class UnlinkMyVeterinaryController {

    private final UnlinkMyVeterinaryUseCase unlinkMyVeterinaryUseCase;

    @DeleteMapping("/{veterinaryId}")
    public ResponseEntity<UnlinkResponse> unlinkMine(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(unlinkMyVeterinaryUseCase.unlinkMine(veterinaryId));
    }
}
