package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.RegisterVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.dto.RegisterVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.RegisterVeterinaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class RegisterVeterinaryController {

    private final RegisterVeterinaryUseCase registerVeterinaryUseCase;

    @PostMapping("/register")
    public ResponseEntity<RegisterVeterinaryResponse> register(@RequestBody RegisterVeterinaryRequest request) {
        RegisterVeterinaryResponse response = registerVeterinaryUseCase.registerVeterinary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
