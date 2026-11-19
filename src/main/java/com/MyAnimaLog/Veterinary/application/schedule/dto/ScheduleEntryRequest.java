package com.MyAnimaLog.Veterinary.application.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una franja tal como la manda el cliente: {@code dayOfWeek} ISO (1 = lunes … 7 = domingo) y horas
 * en {@code "HH:mm"} locales a la zona de la clínica. La misma forma sirve para el horario de la
 * clínica y para el de un empleado.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleEntryRequest {
    private Integer dayOfWeek;
    private String opensAt;
    private String closesAt;
}
