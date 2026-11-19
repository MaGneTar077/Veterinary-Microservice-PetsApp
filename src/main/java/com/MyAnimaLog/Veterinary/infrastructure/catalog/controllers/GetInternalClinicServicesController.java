package com.MyAnimaLog.Veterinary.infrastructure.catalog.controllers;

import com.MyAnimaLog.Veterinary.application.catalog.dto.InternalClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.GetInternalClinicServicesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/veterinaries")
@RequiredArgsConstructor
public class GetInternalClinicServicesController {

    private final GetInternalClinicServicesUseCase getInternalClinicServicesUseCase;

    @GetMapping("/{veterinaryId}/services")
    public ResponseEntity<List<InternalClinicServiceResponse>> list(
            @PathVariable UUID veterinaryId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(getInternalClinicServicesUseCase.list(veterinaryId, activeOnly));
    }
}
