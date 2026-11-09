package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.ListVeterinaryPatientsResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.ListVeterinaryPatientsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class ListVeterinaryPatientsController {

    private final ListVeterinaryPatientsUseCase listVeterinaryPatientsUseCase;

    @GetMapping("/{veterinaryId}/patients")
    public ResponseEntity<ListVeterinaryPatientsResponse> list(
            @PathVariable UUID veterinaryId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(listVeterinaryPatientsUseCase.list(veterinaryId, page, size));
    }
}
