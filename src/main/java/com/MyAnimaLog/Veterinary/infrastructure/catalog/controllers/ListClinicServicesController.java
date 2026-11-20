package com.MyAnimaLog.Veterinary.infrastructure.catalog.controllers;

import com.MyAnimaLog.Veterinary.application.catalog.dto.ListClinicServicesResponse;
import com.MyAnimaLog.Veterinary.application.catalog.ports.in.ListClinicServicesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class ListClinicServicesController {

    private final ListClinicServicesUseCase listClinicServicesUseCase;

    @GetMapping("/{veterinaryId}/services")
    public ResponseEntity<ListClinicServicesResponse> list(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(listClinicServicesUseCase.list(veterinaryId));
    }
}
