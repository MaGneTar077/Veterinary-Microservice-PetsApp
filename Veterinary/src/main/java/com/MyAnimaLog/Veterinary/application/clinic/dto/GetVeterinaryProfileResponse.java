package com.MyAnimaLog.Veterinary.application.clinic.dto;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetVeterinaryProfileResponse {
    private UUID id;
    private String name;
    private String city;
    private String phone;
    private String email;
    private String tenantId;
    private Boolean active;
    private VeterinaryStatus status;
    private String legalName;
    private String nit;
    private String address;
    private String department;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private String currency;
    private Integer defaultAppointmentMinutes;
    private Boolean allowOnlineBooking;
    private Boolean bookingRequiresConfirmation;
    private Integer cancellationMinHours;
    private Boolean directoryVisible;
    private String inviteCode;
    private String inviteLink;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
}
