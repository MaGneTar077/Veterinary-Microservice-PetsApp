package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetMyVeterinariesResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetMyVeterinariesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class GetMyVeterinariesController {

    private final GetMyVeterinariesUseCase getMyVeterinariesUseCase;

    @GetMapping("/me")
    public ResponseEntity<GetMyVeterinariesResponse> getMyVeterinaries() {
        return ResponseEntity.ok(getMyVeterinariesUseCase.getMyVeterinaries());
    }
}
