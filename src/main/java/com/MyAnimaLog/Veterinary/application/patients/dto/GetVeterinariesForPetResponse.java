package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Con qué clínicas comparte el dueño una mascota: la pantalla desde donde puede revocar. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetVeterinariesForPetResponse {

    private UUID petId;
    private List<SharedWith> veterinaries;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SharedWith {
        private UUID veterinaryId;
        private String name;
        private String logoUrl;
        private String city;
        private LocalDateTime grantedAt;
    }
}
