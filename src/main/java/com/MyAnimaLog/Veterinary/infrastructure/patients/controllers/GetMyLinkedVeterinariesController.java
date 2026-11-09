package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetMyLinkedVeterinariesResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetMyLinkedVeterinariesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary/me/linked-veterinaries")
@RequiredArgsConstructor
public class GetMyLinkedVeterinariesController {

    private final GetMyLinkedVeterinariesUseCase getMyLinkedVeterinariesUseCase;

    @GetMapping
    public ResponseEntity<GetMyLinkedVeterinariesResponse> list() {
        return ResponseEntity.ok(getMyLinkedVeterinariesUseCase.list());
    }
}
