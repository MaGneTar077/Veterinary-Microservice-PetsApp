package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Quién comparte sale del token; la clínica, de la ruta. Acá solo viajan las mascotas. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SharePetsWithVeterinaryRequest {
    private List<UUID> petIds;
}
