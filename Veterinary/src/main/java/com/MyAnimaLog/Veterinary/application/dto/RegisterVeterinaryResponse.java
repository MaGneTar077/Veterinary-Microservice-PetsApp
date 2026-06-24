package com.MyAnimaLog.Veterinary.application.dto;

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
public class RegisterVeterinaryResponse {
    private UUID id;
    private String name;
    private String city;
    private String phone;
    private String email;
    private String tenantId;
    private Boolean active;
    private LocalDateTime createdAt;
}
