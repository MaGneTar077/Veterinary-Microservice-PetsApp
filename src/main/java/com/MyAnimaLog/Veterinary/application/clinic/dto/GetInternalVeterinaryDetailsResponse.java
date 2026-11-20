package com.MyAnimaLog.Veterinary.application.clinic.dto;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lo consume Calendar (CALENDAR_REQUESTS_TO_VETERINARY.md, pedido 1). {@code status} trae los 6
 * valores reales de {@link VeterinaryStatus}. <strong>Nunca</strong> incluye NIT, razón social,
 * representante legal ni email: son datos tributarios/personales que Calendar no necesita para
 * decidir reservas en línea, cancelación o zona horaria.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetInternalVeterinaryDetailsResponse {
    private UUID id;
    private String name;
    private VeterinaryStatus status;
    private Boolean allowOnlineBooking;
    private Boolean bookingRequiresConfirmation;
    private Integer cancellationMinHours;
    private String timezone;
    private String address;
    private String phone;
}
