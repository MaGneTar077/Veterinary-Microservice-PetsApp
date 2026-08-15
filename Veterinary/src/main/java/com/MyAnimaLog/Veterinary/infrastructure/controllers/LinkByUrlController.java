package com.MyAnimaLog.Veterinary.infrastructure.controllers;

import com.MyAnimaLog.Veterinary.application.dto.LinkByUrlRequest;
import com.MyAnimaLog.Veterinary.application.dto.LinkByUrlResponse;
import com.MyAnimaLog.Veterinary.application.ports.in.LinkByUrlUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/veterinary/link")
@RequiredArgsConstructor
public class LinkByUrlController {

    private final LinkByUrlUseCase linkByUrlUseCase;

    @PostMapping("/url")
    public ResponseEntity<LinkByUrlResponse> linkByUrl(@RequestBody LinkByUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(linkByUrlUseCase.linkByUrl(request));
    }
}