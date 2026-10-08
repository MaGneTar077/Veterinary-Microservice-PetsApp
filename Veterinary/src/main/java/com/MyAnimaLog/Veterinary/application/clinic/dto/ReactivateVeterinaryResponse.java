package com.MyAnimaLog.Veterinary.application.clinic.dto;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReactivateVeterinaryResponse {
    private UUID id;
    private String name;
    private VeterinaryStatus status;
    private Boolean active;
    private LocalDateTime approvedAt;
}
