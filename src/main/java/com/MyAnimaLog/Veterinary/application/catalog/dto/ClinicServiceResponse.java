package com.MyAnimaLog.Veterinary.application.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Forma pública de un servicio del catálogo (creación, edición, desactivación y cada elemento del
 * listado). Nunca incluye {@code employeeIds}: ese cálculo es solo para el endpoint interno que
 * consume Calendar, no para la pantalla de administración de la clínica.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClinicServiceResponse {
    private UUID id;
    private UUID veterinaryId;
    private String name;
    private String category;
    private Integer durationMinutes;
    private BigDecimal price;
    private Boolean bookableOnline;
    private Boolean active;
    private Boolean requiresLicense;
    private List<String> allowedRoles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
