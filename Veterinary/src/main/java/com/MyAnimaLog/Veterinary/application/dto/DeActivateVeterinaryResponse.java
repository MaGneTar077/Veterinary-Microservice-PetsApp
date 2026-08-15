package com.MyAnimaLog.Veterinary.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeActivateVeterinaryResponse {
    private UUID id;
    private String name;
    private Boolean active;
}
