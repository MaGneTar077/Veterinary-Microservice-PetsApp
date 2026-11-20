package com.MyAnimaLog.Veterinary.application.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListClinicServicesResponse {

    private List<Item> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private UUID id;
        private String name;
        private String category;
        private Integer durationMinutes;
        private BigDecimal price;
        private Boolean bookableOnline;
        private Boolean active;
        private Boolean requiresLicense;
        private List<String> allowedRoles;
    }
}
