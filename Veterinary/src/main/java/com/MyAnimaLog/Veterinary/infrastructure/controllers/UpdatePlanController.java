package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.UpdatePlanRequest;
import com.MyAnimaLog.Veterinary.application.dto.UpdatePlanResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.UpdatePlanUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class UpdatePlanController {

    private final UpdatePlanUseCase updatePlanUseCase;

    @PatchMapping("/{veterinaryId}/subscription")
    public ResponseEntity<UpdatePlanResponse> updatePlan(
            @PathVariable UUID veterinaryId,
            @RequestBody UpdatePlanRequest request) {
        return ResponseEntity.ok(updatePlanUseCase.updatePlan(veterinaryId, request));
    }
}
