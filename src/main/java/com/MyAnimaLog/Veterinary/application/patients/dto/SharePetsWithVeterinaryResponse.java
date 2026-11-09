package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * {@code alreadyShared} distingue las que ya estaban compartidas de las que se compartieron en esta
 * llamada: compartir es idempotente, así que repetir la operación no es un error, pero el frontend
 * necesita poder decir "2 nuevas" en vez de "3 compartidas" cada vez.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SharePetsWithVeterinaryResponse {

    private UUID veterinaryId;
    private String veterinaryName;
    private int sharedCount;
    private List<SharedPet> pets;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SharedPet {
        private UUID petId;
        private String petName;
        private String species;
        private LocalDateTime grantedAt;
        private boolean alreadyShared;
    }
}
