package com.MyAnimaLog.Veterinary.application.dto;

import com.MyAnimaLog.Veterinary.domain.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEmployeeRoleRequest {
    private EmployeeRole role;
}
