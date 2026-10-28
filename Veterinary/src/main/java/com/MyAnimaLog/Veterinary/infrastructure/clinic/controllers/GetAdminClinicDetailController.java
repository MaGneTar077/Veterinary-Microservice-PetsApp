package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicDetailResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetAdminClinicDetailUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/admin/veterinaries")
@RequiredArgsConstructor
public class GetAdminClinicDetailController {

    private final GetAdminClinicDetailUseCase getAdminClinicDetailUseCase;

    @GetMapping("/{veterinaryId}")
    public ResponseEntity<AdminClinicDetailResponse> getDetail(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(getAdminClinicDetailUseCase.getDetail(veterinaryId));
    }
}
