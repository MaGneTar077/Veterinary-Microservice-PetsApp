package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetLinkCodeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.GetLinkCodeUseCase;
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
public class GetLinkCodeController {

    private final GetLinkCodeUseCase getLinkCodeUseCase;

    @GetMapping("/{veterinaryId}/link-code")
    public ResponseEntity<GetLinkCodeResponse> getLinkCode(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(getLinkCodeUseCase.getLinkCode(veterinaryId));
    }
}
