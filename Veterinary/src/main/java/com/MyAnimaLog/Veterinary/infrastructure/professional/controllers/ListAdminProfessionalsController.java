package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalListResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.ListAdminProfessionalsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary/admin/professionals")
@RequiredArgsConstructor
public class ListAdminProfessionalsController {

    private final ListAdminProfessionalsUseCase listAdminProfessionalsUseCase;

    @GetMapping
    public ResponseEntity<AdminProfessionalListResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(listAdminProfessionalsUseCase.list(search, verified, page, size));
    }
}
