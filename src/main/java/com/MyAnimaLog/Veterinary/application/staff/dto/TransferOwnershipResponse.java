package com.MyAnimaLog.Veterinary.application.staff.dto;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Resultado de la transferencia. {@code legalRepresentativeChanged} es siempre {@code false}: los
 * datos del representante legal de la clínica (verificación, Fase 2B) <strong>no</strong> cambian
 * con la propiedad. Si el nuevo dueño también es el nuevo representante legal, hay que actualizarlo
 * aparte con {@code PUT /api/veterinary/{id}/legal-representative}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferOwnershipResponse {

    private UUID veterinaryId;
    private Party previousOwner;
    private Party newOwner;
    private boolean legalRepresentativeChanged;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Party {
        private UUID employeeId;
        private UUID userId;
        private EmployeeRole role;
    }
}
