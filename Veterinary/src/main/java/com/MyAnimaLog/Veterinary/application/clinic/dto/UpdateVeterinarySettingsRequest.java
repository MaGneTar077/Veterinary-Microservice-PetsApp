package com.MyAnimaLog.Veterinary.application.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateVeterinarySettingsRequest {
    private String timezone;
    private Integer defaultAppointmentMinutes;
    private Boolean allowOnlineBooking;
    private Boolean bookingRequiresConfirmation;
    private Integer cancellationMinHours;
    private Boolean directoryVisible;
}
