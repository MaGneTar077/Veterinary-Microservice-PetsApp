package com.MyAnimaLog.Veterinary.infrastructure.patients.controllers;

import com.MyAnimaLog.Veterinary.application.patients.dto.ListClientsResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.ListClientsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class ListClientsController {

    private final ListClientsUseCase listClientsUseCase;

    @GetMapping("/{veterinaryId}/clients")
    public ResponseEntity<ListClientsResponse> list(
            @PathVariable UUID veterinaryId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(listClientsUseCase.list(veterinaryId, page, size));
    }
}
