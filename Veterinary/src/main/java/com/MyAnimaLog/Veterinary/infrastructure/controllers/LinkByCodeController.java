package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.LinkByCodeRequest;
import com.MyAnimaLog.Veterinary.application.dto.LinkByCodeResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.LinkByCodeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/veterinary/link")
@RequiredArgsConstructor
public class LinkByCodeController {

    private final LinkByCodeUseCase linkByCodeUseCase;

    @PostMapping("/code")
    public ResponseEntity<LinkByCodeResponse> linkByCode(@RequestBody LinkByCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(linkByCodeUseCase.linkByCode(request));
    }
}