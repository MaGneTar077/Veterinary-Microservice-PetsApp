package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicListResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.ListAdminClinicsUseCase;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary/admin/veterinaries")
@RequiredArgsConstructor
public class ListAdminClinicsController {

    private final ListAdminClinicsUseCase listAdminClinicsUseCase;

    @GetMapping
    public ResponseEntity<AdminClinicListResponse> list(
            @RequestParam(required = false) VeterinaryStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(listAdminClinicsUseCase.list(status, search, plan, city, department, page, size));
    }
}
