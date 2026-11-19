package com.MyAnimaLog.Veterinary.infrastructure.catalog.controllers;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.dto.UpdateClinicServiceRequest;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.UpdateClinicServiceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class UpdateClinicServiceController {

    private final UpdateClinicServiceUseCase updateClinicServiceUseCase;

    @PatchMapping("/{veterinaryId}/services/{serviceId}")
    public ResponseEntity<ClinicServiceResponse> update(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID serviceId,
            @RequestBody UpdateClinicServiceRequest request) {
        return ResponseEntity.ok(updateClinicServiceUseCase.update(veterinaryId, serviceId, request));
    }
}
