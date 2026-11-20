package com.MyAnimaLog.Veterinary.application.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Las filas propias del empleado, tal cual están guardadas. {@code items} vacío
 * ({@code usesClinicHours = true}) significa <strong>"usa el horario de la clínica"</strong>, no
 * "no atiende nunca".
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeScheduleResponse {
    private UUID employeeId;
    private String timezone;
    private boolean usesClinicHours;
    private List<ScheduleEntryResponse> items;
}
