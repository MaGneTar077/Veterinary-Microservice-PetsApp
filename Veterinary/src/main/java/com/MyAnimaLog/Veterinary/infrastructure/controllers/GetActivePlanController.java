package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.GetActivePlanResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.GetActivePlanUseCase;
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