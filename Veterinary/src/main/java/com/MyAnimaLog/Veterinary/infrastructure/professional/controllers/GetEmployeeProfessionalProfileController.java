package com.MyAnimaLog.Veterinary.infrastructure.professional.controllers;

import com.MyAnimaLog.Veterinary.application.professional.dto.EmployeeProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.in.GetEmployeeProfessionalProfileUseCase;
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
public class GetEmployeeProfessionalProfileController {

    private final GetEmployeeProfessionalProfileUseCase getEmployeeProfessionalProfileUseCase;

    @GetMapping("/{veterinaryId}/employees/{employeeId}/professional-profile")
    public ResponseEntity<EmployeeProfessionalProfileResponse> getProfile(@PathVariable UUID veterinaryId,
                                                                            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(getEmployeeProfessionalProfileUseCase.getProfile(veterinaryId, employeeId));
    }
}
