package com.MyAnimaLog.Veterinary.application.staff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferOwnershipRequest {
    /** Empleado activo de la misma clínica que pasa a ser OWNER. */
    private UUID newOwnerEmployeeId;
}
