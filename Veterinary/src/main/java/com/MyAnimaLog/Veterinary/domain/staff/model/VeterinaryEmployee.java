package com.MyAnimaLog.Veterinary.domain.staff.model;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class VeterinaryEmployee {
    private UUID id;
    private UUID veterinaryId;
    private UUID userId;
    private EmployeeRole role;
    private Boolean active;
    private LocalDateTime createdAt;
    /**
     * Cuándo se retiró a este empleado. Retirar es {@code active=false} + esta fecha, nunca un
     * DELETE: las historias clínicas que firmó deben seguir siendo atribuibles. Se limpia a
     * {@code null} si el empleado vuelve (reactivación o aceptación de una invitación nueva).
     */
    private LocalDateTime removedAt;
}
