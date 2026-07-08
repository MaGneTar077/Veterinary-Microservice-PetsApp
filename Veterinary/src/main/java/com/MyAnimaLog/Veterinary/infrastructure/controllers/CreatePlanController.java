package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.CreatePlanRequest;
import com.MyAnimaLog.Veterinary.application.dto.CreatePlanResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.CreatePlanUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class CreatePlanController {
    private final CreatePlanUseCase createPlanUseCase;

    @PostMapping("/{veterinaryId}/subscription")
    public ResponseEntity<CreatePlanResponse> createPlan(
            @PathVariable UUID veterinaryId,
            @RequestBody CreatePlanRequest request) {
        request.setVeterinaryId(veterinaryId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createPlanUseCase.createPlan(request));
    }
}
