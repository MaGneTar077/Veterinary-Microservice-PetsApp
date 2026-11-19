package com.MyAnimaLog.Veterinary.application.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Forma exacta que consume Calendar (CALENDAR_REQUESTS_TO_VETERINARY.md, pedido 3):
 * {@code {timezone, businessHours[], employeeHours[], blocks[]}}. Las horas van como
 * {@code "HH:mm"} locales a {@code timezone}; los bloqueos, como instantes ISO con zona.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetScheduleRulesResponse {

    private String timezone;
    private List<BusinessHour> businessHours;
    private List<EmployeeHour> employeeHours;
    private List<Block> blocks;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BusinessHour {
        private Integer dayOfWeek;
        private String opensAt;
        private String closesAt;
    }

    /** Horas <strong>efectivas</strong> del empleado — ver el Javadoc de {@code GetScheduleRulesService}. */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmployeeHour {
        private UUID employeeId;
        private Integer dayOfWeek;
        private String opensAt;
        private String closesAt;
    }

    /** {@code employeeId == null} = bloqueo de toda la clínica. */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Block {
        private UUID employeeId;
        private OffsetDateTime startsAt;
        private OffsetDateTime endsAt;
        private String reason;
    }
}
