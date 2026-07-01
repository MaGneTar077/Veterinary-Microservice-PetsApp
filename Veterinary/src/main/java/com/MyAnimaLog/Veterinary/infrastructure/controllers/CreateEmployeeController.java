package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.CreateEmployeeRequest;
import com.MyAnimaLog.Veterinary.application.dto.CreateEmployeeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.CreateEmployeeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinary/employees")
@RequiredArgsConstructor
public class CreateEmployeeController {

    private final CreateEmployeeUseCase createEmployeeUseCase;

    @PostMapping
    public ResponseEntity<CreateEmployeeResponse> create(@RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createEmployeeUseCase.create(request));
    }
}
