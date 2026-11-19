package com.MyAnimaLog.Veterinary.application.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Edición parcial: un campo {@code null} deja el valor actual sin cambios (mismo patrón que UpdateVeterinarySettingsRequest). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateClinicServiceRequest {
    private String name;
    private String category;
    private Integer durationMinutes;
    private BigDecimal price;
    private Boolean bookableOnline;
    private Boolean requiresLicense;
    private List<String> allowedRoles;
}
