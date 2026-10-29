package com.MyAnimaLog.Veterinary.application.staff.ports.out;

import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeterinaryEmployeeRepositoryPort {
    VeterinaryEmployee save(VeterinaryEmployee employee);
    boolean existsByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
    Optional<VeterinaryEmployee> findById(UUID id);
    Optional<VeterinaryEmployee> findByVeterinaryIdAndUserId(UUID veterinaryId, UUID userId);
    List<VeterinaryEmployee> findByUserIdAndRole(UUID userId, EmployeeRole role);
    List<VeterinaryEmployee> findByUserIdAndActiveTrue(UUID userId);

    /** All employees of a single clinic, for the admin detail screen. */
    List<VeterinaryEmployee> findByVeterinaryId(UUID veterinaryId);

    /** All employees across a batch of clinics, for the admin list screen (avoids N+1 per row). */
    List<VeterinaryEmployee> findByVeterinaryIdIn(List<UUID> veterinaryIds);

    /** The clinic's active employee with the given role (used to resolve the OWNER). */
    Optional<VeterinaryEmployee> findByVeterinaryIdAndRoleAndActiveTrue(UUID veterinaryId, EmployeeRole role);

    /** Active employments across a batch of users, for the admin professionals list (avoids N+1 per row). */
    List<VeterinaryEmployee> findByUserIdInAndActiveTrue(List<UUID> userIds);

    /**
     * Rol y estado reales del caller sobre una clínica, en una sola consulta (join de
     * {@code veterinary_employee} con {@code veterinary}). Es la base de
     * {@code VeterinaryAuthorizationService}: los claims {@code vet_role}/{@code vet_status} de un
     * token de clínica pueden tener hasta 60 min de antigüedad, así que ninguna decisión de
     * autorización se toma con ellos — se toman con esta fila.
     *
     * <p>Reemplaza (no agrega) la lectura de la clínica que antes hacía {@code requireActiveMember}
     * solo para detectar la suspensión: el estado viene en la misma fila.
     *
     * <p>{@code Optional.empty()} significa que el usuario no tiene ningún registro de empleado en
     * esa clínica (o que la clínica no existe). Una fila con {@code active=false} es un empleado
     * retirado: existe, pero no puede operar.
     */
    Optional<ClinicMembership> findClinicMembership(UUID veterinaryId, UUID userId);

    /**
     * Cambia el rol de un empleado activo <strong>solo si</strong> su rol en la base sigue siendo
     * {@code expectedRole} ({@code UPDATE ... WHERE id = ? AND role = ? AND active = true}, mismo
     * patrón que {@code VeterinaryRepositoryPort.claimStatusTransition}). Devuelve {@code false} si
     * no actualizó ninguna fila, es decir si alguien más cambió ese rol en el medio: el caller debe
     * tratarlo como un conflicto, nunca seguir de largo.
     *
     * <p>Lo usa la transferencia de propiedad, que son dos de estos encadenados en una transacción.
     */
    boolean claimRoleTransition(UUID employeeId, EmployeeRole expectedRole, EmployeeRole newRole);

    /**
     * @param employeeId id del registro de empleado (lo que el token llama {@code employee_id})
     * @param role rol actual en la base, no el del claim
     * @param active {@code false} si el empleado fue retirado
     * @param veterinaryStatus estado actual de la clínica, no el del claim
     */
    record ClinicMembership(UUID employeeId, EmployeeRole role, boolean active,
                             VeterinaryStatus veterinaryStatus) {
    }
}
