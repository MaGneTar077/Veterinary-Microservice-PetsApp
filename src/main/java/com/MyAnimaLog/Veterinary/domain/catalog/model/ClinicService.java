package com.MyAnimaLog.Veterinary.domain.catalog.model;

import com.MyAnimaLog.Veterinary.domain.catalog.enums.ServiceCategory;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Un servicio del catálogo de una clínica (consulta, vacunación, cirugía...). Nunca se borra:
 * {@code DeactivateClinicService} solo apaga {@code active}, porque puede haber citas ya creadas
 * (en Calendar) que lo referencian por id.
 *
 * <p>{@code allowedRoles} + {@code requiresLicense} son los dos datos con los que Veterinary calcula
 * {@code employeeIds} para el endpoint interno que consume Calendar — nunca los calcula Calendar, y
 * nunca se exponen el email o el teléfono de un empleado para eso.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ClinicService {
    private UUID id;
    private UUID veterinaryId;
    private String name;
    private ServiceCategory category;
    private Integer durationMinutes;
    private BigDecimal price;

    @Builder.Default
    private Boolean bookableOnline = true;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Boolean requiresLicense = false;

    @Builder.Default
    private List<EmployeeRole> allowedRoles = List.of(EmployeeRole.OWNER, EmployeeRole.ADMIN, EmployeeRole.VETERINARIAN);

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
