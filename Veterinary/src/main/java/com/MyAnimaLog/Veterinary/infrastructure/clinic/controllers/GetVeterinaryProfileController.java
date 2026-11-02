package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetVeterinaryProfileResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetVeterinaryProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class GetVeterinaryProfileController {

    private final GetVeterinaryProfileUseCase getVeterinaryProfileUseCase;

    @GetMapping("/{veterinaryId}")
    public ResponseEntity<GetVeterinaryProfileResponse> getProfile(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(getVeterinaryProfileUseCase.getProfile(veterinaryId));
    }
}
