package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.LeaveVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.LeaveVeterinaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class LeaveVeterinaryController {

    private final LeaveVeterinaryUseCase leaveVeterinaryUseCase;

    @PostMapping("/{veterinaryId}/leave")
    public ResponseEntity<LeaveVeterinaryResponse> leave(@PathVariable UUID veterinaryId) {
        return ResponseEntity.ok(leaveVeterinaryUseCase.leave(veterinaryId));
    }
}
