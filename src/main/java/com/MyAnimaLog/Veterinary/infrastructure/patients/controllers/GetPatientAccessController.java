package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetPatientAccessResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetPatientAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/veterinaries")
@RequiredArgsConstructor
public class GetPatientAccessController {

    private final GetPatientAccessUseCase getPatientAccessUseCase;

    @GetMapping("/{veterinaryId}/patients/{petId}/access")
    public ResponseEntity<GetPatientAccessResponse> getAccess(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID petId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(getPatientAccessUseCase.getAccess(veterinaryId, petId, userId));
    }
}
