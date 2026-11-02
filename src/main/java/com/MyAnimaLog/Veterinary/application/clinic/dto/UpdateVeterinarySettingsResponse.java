package com.MyAnimaLog.Veterinary.application.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateVeterinarySettingsResponse {
    private UUID id;
    private String timezone;
    private Integer defaultAppointmentMinutes;
    private Boolean allowOnlineBooking;
    private Boolean bookingRequiresConfirmation;
    private Integer cancellationMinHours;
    private Boolean directoryVisible;
}
