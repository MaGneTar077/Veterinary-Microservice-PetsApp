package com.MyAnimaLog.Veterinary.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class VeterinarySubscription {
    private UUID id;
    private UUID veterinaryId;
    private String plan;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
}
