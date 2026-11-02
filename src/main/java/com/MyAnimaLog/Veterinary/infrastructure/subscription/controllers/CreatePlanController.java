package com.MyAnimaLog.Veterinary.infrastructure.subscription.controllers;

import com.MyAnimaLog.Veterinary.application.subscription.dto.CreatePlanRequest;
import com.MyAnimaLog.Veterinary.application.subscription.dto.CreatePlanResponse;
import com.MyAnimaLog.Veterinary.application.subscription.ports.in.CreatePlanUseCase;
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
