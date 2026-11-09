package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Pacientes compartidos activamente con la clínica. Los datos de la mascota ({@code name},
 * {@code species}, {@code breed}, {@code photoUrl}) salen de Pets EN VIVO, en una sola llamada en
 * lote para toda la página — nunca del snapshot de {@code veterinary_patients}, que es solo
 * trazabilidad.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListVeterinaryPatientsResponse {

    private List<Patient> items;
    private int page;
    private int size;
    private long total;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Patient {
        private UUID petId;
        private String name;
        private String species;
        private String breed;
        private String photoUrl;
        private UUID ownerUserId;
        private LocalDateTime grantedAt;
    }
}
