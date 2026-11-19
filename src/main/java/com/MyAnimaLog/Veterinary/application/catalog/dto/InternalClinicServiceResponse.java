package com.MyAnimaLog.Veterinary.application.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Forma que consume Calendar (CALENDAR_REQUESTS_TO_VETERINARY.md, pedido 2). {@code employeeIds} lo
 * calcula Veterinary — Calendar solo lo recibe, nunca el rol/licencia de cada empleado. Sin email ni
 * teléfono.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InternalClinicServiceResponse {
    private UUID id;
    private String name;
    private String category;
    private Integer durationMinutes;
    private BigDecimal price;
    private Boolean active;
    private Boolean bookableOnline;
    private Boolean requiresLicense;
    private List<UUID> employeeIds;
}
