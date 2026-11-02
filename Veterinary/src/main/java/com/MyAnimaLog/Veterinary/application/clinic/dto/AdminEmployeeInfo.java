package com.MyAnimaLog.Veterinary.application.clinic.dto;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminEmployeeInfo {
    private UUID employeeId;
    private UUID userId;
    private EmployeeRole role;
    private Boolean active;
}
