package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Detalle de un paciente: datos de Pets en vivo, solo si está compartido activamente. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetVeterinaryPatientResponse {
    private UUID petId;
    private String name;
    private String species;
    private String breed;
    private String sex;
    private LocalDate birthDate;
    private String photoUrl;
    private UUID ownerUserId;
    private LocalDateTime grantedAt;
}
