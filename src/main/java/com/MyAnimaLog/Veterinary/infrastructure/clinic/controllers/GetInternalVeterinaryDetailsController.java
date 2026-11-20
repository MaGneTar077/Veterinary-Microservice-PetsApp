package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetInternalVeterinaryDetailsResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetInternalVeterinaryDetailsUseCase;
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
public class GetInternalVeterinaryDetailsController {

    private final GetInternalVeterinaryDetailsUseCase getInternalVeterinaryDetailsUseCase;

    @GetMapping("/{veterinaryId}")
    public ResponseEntity<GetInternalVeterinaryDetailsResponse> get(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(getInternalVeterinaryDetailsUseCase.get(veterinaryId));
    }
}
