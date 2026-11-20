package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetSharedWithVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetSharedWithVeterinaryUseCase;
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
public class GetSharedWithVeterinaryController {

    private final GetSharedWithVeterinaryUseCase getSharedWithVeterinaryUseCase;

    @GetMapping("/{veterinaryId}/patients/{petId}/shared")
    public ResponseEntity<GetSharedWithVeterinaryResponse> get(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID petId) {
        return ResponseEntity.ok(getSharedWithVeterinaryUseCase.get(veterinaryId, petId));
    }
}
