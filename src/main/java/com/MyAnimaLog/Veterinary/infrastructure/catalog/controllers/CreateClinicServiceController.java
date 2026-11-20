package com.MyAnimaLog.Veterinary.infrastructure.catalog.controllers;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ClinicServiceResponse;
import com.MyAnimaLog.Veterinary.application.catalog.dto.CreateClinicServiceRequest;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.CreateClinicServiceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class CreateClinicServiceController {

    private final CreateClinicServiceUseCase createClinicServiceUseCase;

    @PostMapping("/{veterinaryId}/services")
    public ResponseEntity<ClinicServiceResponse> create(
            @PathVariable UUID veterinaryId,
            @RequestBody CreateClinicServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createClinicServiceUseCase.create(veterinaryId, request));
    }
}
