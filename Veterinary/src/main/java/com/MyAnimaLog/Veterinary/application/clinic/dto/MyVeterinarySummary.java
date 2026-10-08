package com.MyAnimaLog.Veterinary.application.clinic.dto;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
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
public class MyVeterinarySummary {
    private UUID veterinaryId;
    private String name;
    private EmployeeRole role;
    private VeterinaryStatus status;
}
