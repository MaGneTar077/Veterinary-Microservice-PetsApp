package com.MyAnimaLog.Veterinary.application.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateClinicServiceRequest {
    private String name;
    private String category;
    private Integer durationMinutes;
    private BigDecimal price;
    private Boolean bookableOnline;
    private Boolean requiresLicense;
    private List<String> allowedRoles;
}
