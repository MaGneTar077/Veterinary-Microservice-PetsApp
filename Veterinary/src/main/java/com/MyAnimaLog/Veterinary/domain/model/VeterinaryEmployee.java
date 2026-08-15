package com.MyAnimaLog.Veterinary.domain.model;

import com.MyAnimaLog.Veterinary.domain.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class VeterinaryEmployee {
    private UUID id;
    private UUID veterinaryId;
    private UUID userId;
    private EmployeeRole role;
    private Boolean active;
    private LocalDateTime createdAt;
}
