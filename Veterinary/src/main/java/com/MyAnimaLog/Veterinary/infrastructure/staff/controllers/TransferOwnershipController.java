package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.TransferOwnershipRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.TransferOwnershipResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.TransferOwnershipUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class TransferOwnershipController {

    private final TransferOwnershipUseCase transferOwnershipUseCase;

    @PostMapping("/{veterinaryId}/ownership-transfer")
    public ResponseEntity<TransferOwnershipResponse> transfer(
            @PathVariable UUID veterinaryId,
            @RequestBody TransferOwnershipRequest request) {
        return ResponseEntity.ok(transferOwnershipUseCase.transfer(veterinaryId, request));
    }
}
