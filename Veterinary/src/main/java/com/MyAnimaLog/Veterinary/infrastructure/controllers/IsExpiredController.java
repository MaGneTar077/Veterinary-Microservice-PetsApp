package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.IsExpiredResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.IsExpiredUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class IsExpiredController {

    private final IsExpiredUseCase isExpiredUseCase;

    @GetMapping("/{veterinaryId}/subscription/is-expired")
    public ResponseEntity<IsExpiredResponse> isExpired(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(isExpiredUseCase.isExpired(veterinaryId));
    }
}
