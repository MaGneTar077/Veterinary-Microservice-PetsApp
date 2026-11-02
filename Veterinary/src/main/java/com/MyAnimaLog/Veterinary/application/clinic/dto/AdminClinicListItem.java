package com.MyAnimaLog.Veterinary.application.clinic.dto;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminClinicListItem {
    private UUID id;
    private String name;
    private String legalName;
    private String nit;
    private String city;
    private String department;
    private VeterinaryStatus status;
    private String plan;
    private String legalRepName;
    private int employeesCount;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime suspendedAt;
}
