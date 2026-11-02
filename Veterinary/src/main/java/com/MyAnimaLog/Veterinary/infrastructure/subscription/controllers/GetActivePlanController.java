package com.MyAnimaLog.Veterinary.infrastructure.subscription.controllers;

import com.MyAnimaLog.Veterinary.application.subscription.dto.GetActivePlanResponse;
import com.MyAnimaLog.Veterinary.application.subscription.ports.in.GetActivePlanUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class GetActivePlanController {

    private final GetActivePlanUseCase getActivePlanUseCase;

    @GetMapping("/{veterinaryId}/subscription/active")
    public ResponseEntity<GetActivePlanResponse> getActivePlan(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(getActivePlanUseCase.getActivePlan(veterinaryId));
    }
}