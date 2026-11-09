package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinaryPatientResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetVeterinaryPatientUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class GetVeterinaryPatientController {

    private final GetVeterinaryPatientUseCase getVeterinaryPatientUseCase;

    @GetMapping("/{veterinaryId}/patients/{petId}")
    public ResponseEntity<GetVeterinaryPatientResponse> get(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID petId) {
        return ResponseEntity.ok(getVeterinaryPatientUseCase.get(veterinaryId, petId));
    }
}
