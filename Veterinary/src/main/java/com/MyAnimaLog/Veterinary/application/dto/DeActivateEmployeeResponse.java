package com.MyAnimaLog.Veterinary.application.dto;

import com.MyAnimaLog.Veterinary.domain.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeActivateEmployeeResponse {
    private UUID id;
    private UUID veterinaryId;
    private UUID userId;
    private EmployeeRole role;
    private Boolean active;
}
