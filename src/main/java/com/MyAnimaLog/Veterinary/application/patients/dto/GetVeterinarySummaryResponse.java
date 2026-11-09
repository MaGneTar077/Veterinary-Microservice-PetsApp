package com.MyAnimaLog.Veterinary.application.patients.dto;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetVeterinarySummaryResponse {
    private UUID id;
    private String name;
    private String timezone;
    private VeterinaryStatus status;
    private String logoUrl;
}
