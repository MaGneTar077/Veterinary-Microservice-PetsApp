package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.InternalVeterinaryMemberResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.GetInternalVeterinaryMembersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/veterinaries")
@RequiredArgsConstructor
public class GetInternalVeterinaryMembersController {

    private final GetInternalVeterinaryMembersUseCase getInternalVeterinaryMembersUseCase;

    @GetMapping("/{veterinaryId}/members")
    public ResponseEntity<List<InternalVeterinaryMemberResponse>> list(
            @PathVariable UUID veterinaryId,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(getInternalVeterinaryMembersUseCase.list(veterinaryId, includeInactive));
    }
}
