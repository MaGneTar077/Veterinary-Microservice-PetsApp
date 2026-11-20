package com.MyAnimaLog.Veterinary.application.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** {@code items} vacío significa que la clínica no tiene horario configurado. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BusinessHoursResponse {
    private String timezone;
    private List<ScheduleEntryResponse> items;
}
