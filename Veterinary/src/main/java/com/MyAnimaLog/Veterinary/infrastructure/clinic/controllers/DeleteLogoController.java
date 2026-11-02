package com.MyAnimaLog.Veterinary.infrastructure.clinic.controllers;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeleteLogoResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.in.DeleteLogoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class DeleteLogoController {

    private final DeleteLogoUseCase deleteLogoUseCase;

    @DeleteMapping("/{veterinaryId}/logo")
    public ResponseEntity<DeleteLogoResponse> delete(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(deleteLogoUseCase.deleteLogo(veterinaryId));
    }
}
