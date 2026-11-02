package com.MyAnimaLog.Veterinary.application.staff.dto;

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
public class UpdateEmployeeRoleResponse {
    private UUID id;
    private UUID veterinaryId;
    private UUID userId;
    private EmployeeRole role;
}
