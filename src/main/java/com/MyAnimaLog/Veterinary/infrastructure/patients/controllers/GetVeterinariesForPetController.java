package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinariesForPetResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.GetVeterinariesForPetUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary/me/pets")
@RequiredArgsConstructor
public class GetVeterinariesForPetController {

    private final GetVeterinariesForPetUseCase getVeterinariesForPetUseCase;

    @GetMapping("/{petId}/veterinaries")
    public ResponseEntity<GetVeterinariesForPetResponse> list(@PathVariable UUID petId) {
        return ResponseEntity.ok(getVeterinariesForPetUseCase.list(petId));
    }
}
