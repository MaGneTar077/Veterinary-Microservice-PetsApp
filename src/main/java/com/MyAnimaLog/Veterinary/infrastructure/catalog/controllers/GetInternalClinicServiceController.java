package com.MyAnimaLog.Veterinary.infrastructure.catalog.controllers;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.GetInternalClinicServiceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/veterinaries")
@RequiredArgsConstructor
public class GetInternalClinicServiceController {

    private final GetInternalClinicServiceUseCase getInternalClinicServiceUseCase;

    @GetMapping("/{veterinaryId}/services/{serviceId}")
    public ResponseEntity<InternalClinicServiceResponse> get(
            @PathVariable UUID veterinaryId,
            @PathVariable UUID serviceId) {
        return ResponseEntity.ok(getInternalClinicServiceUseCase.get(veterinaryId, serviceId));
    }
}
