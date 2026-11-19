package com.MyAnimaLog.Veterinary.infrastructure.catalog.controllers;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.DeactivateClinicServiceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class DeactivateClinicServiceController {

    private final DeactivateClinicServiceUseCase deactivateClinicServiceUseCase;

    @PatchMapping("/{veterinaryId}/services/{serviceId}/deactivate")
    public ResponseEntity<ClinicServiceResponse> deactivate(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID serviceId) {
        return ResponseEntity.ok(deactivateClinicServiceUseCase.deactivate(veterinaryId, serviceId));
    }
}
