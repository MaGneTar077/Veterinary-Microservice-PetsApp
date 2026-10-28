package com.MyAnimaLog.Veterinary.application.clinic.dto;

import com.MyAnimaLog.Veterinary.application.verification.dto.AdminVerificationHistoryItem;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminClinicDetailResponse {
    private UUID id;
    private String name;
    private String legalName;
    private String nit;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String department;
    private VeterinaryStatus status;
    private String plan;
    private String ownerName;
    private String ownerEmail;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime suspendedAt;
    private String suspensionReason;
    private List<AdminEmployeeInfo> employees;
    private List<AdminVerificationHistoryItem> verificationHistory;
}
