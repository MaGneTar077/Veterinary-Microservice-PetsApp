package com.MyAnimaLog.Veterinary.infrastructure.staff.controllers;

import com.MyAnimaLog.Veterinary.application.staff.dto.ListEmployeesResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.in.ListEmployeesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/veterinary")
@RequiredArgsConstructor
public class ListEmployeesController {

    private final ListEmployeesUseCase listEmployeesUseCase;

    @GetMapping("/{veterinaryId}/employees")
    public ResponseEntity<ListEmployeesResponse> list(
            @PathVariable UUID veterinaryId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(listEmployeesUseCase.list(veterinaryId, includeInactive));
    }
}
