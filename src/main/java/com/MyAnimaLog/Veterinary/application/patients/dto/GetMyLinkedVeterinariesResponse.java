package com.MyAnimaLog.Veterinary.application.patients.dto;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetMyLinkedVeterinariesResponse {

    private List<LinkedVeterinary> veterinaries;

    /**
     * {@code sharedPetsCount} es cuántas mascotas <strong>este</strong> dueño le comparte a
     * <strong>esa</strong> clínica ahora mismo. Puede ser {@code 0}: estar vinculado no comparte
     * ninguna mascota por sí solo.
     *
     * <p>{@code status} viene para que el frontend pueda avisar que una clínica quedó suspendida
     * (el dueño seguiría viéndola en su lista, pero no podría pedir cita).
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LinkedVeterinary {
        private UUID veterinaryId;
        private String name;
        private String logoUrl;
        private String city;
        private String phone;
        private VeterinaryStatus status;
        private LocalDateTime linkedAt;
        private long sharedPetsCount;
    }
}
