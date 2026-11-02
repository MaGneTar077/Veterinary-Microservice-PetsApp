package com.MyAnimaLog.Veterinary.application.staff.dto;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * El equipo de la clínica. {@code name} y {@code email} se resuelven contra el User service y son
 * best-effort: si no se pueden resolver llegan en {@code null} y el frontend muestra el rol igual
 * (ver {@code UserServicePort.getUsers}).
 *
 * <p>No incluye datos clínicos ni documento de identidad de nadie. El estado de la licencia de un
 * veterinario se consulta aparte, en
 * {@code GET /api/veterinary/{id}/employees/{employeeId}/professional-profile}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListEmployeesResponse {

    private List<Employee> employees;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Employee {
        private UUID employeeId;
        private UUID userId;
        private String name;
        private String email;
        private EmployeeRole role;
        private Boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime removedAt;
        /** {@code true} en la fila del propio caller, para que el frontend no le ofrezca retirarse a sí mismo. */
        private boolean self;
    }
}
