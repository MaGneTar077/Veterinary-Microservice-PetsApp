package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import com.MyAnimaLog.Veterinary.infrastructure.security.InvitationTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equivalente de {@code Fase2BNewTablesPersistenceIntegrationTest} para las tablas de la Fase 3
 * ({@code 007_staff_invitations.sql}): guarda el modelo de dominio con solo los campos que setea un
 * servicio real, por el camino Port -> Mapper -> Entity -> Hibernate -> Supabase, para detectar el
 * tipo de bug que los tests con Mockito no pueden ver (una columna mal mapeada, o una
 * {@code NOT NULL DEFAULT} de la base que Hibernate pisa con un {@code null} explícito porque el
 * modelo de dominio no tiene {@code @Builder.Default} — ver CLAUDE.md).
 *
 * <p>{@code @Transactional} revierte todo al final de cada test: no queda nada en Supabase.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Fase3StaffTablesPersistenceIntegrationTest {

    @Autowired
    private EmployeeInvitationRepositoryPort invitationRepositoryPort;

    @Autowired
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Autowired
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    /**
     * Clínica real a la que apuntar: {@code employee_invitations.veterinary_id} tiene FK contra
     * {@code public.veterinary}, y la FK se valida en el flush, no al llamar {@code save(...)}.
     * Con un UUID inventado el insert falla con
     * {@code employee_invitations_veterinary_id_fkey} en cuanto algo fuerza el flush (p. ej. la
     * lectura posterior) — así que cada test crea su propia clínica dentro de la misma transacción
     * que luego se revierte.
     */
    private UUID persistClinic() {
        return veterinaryRepositoryPort.save(Veterinary.builder()
                .id(UUID.randomUUID())
                .name("Clínica de prueba " + UUID.randomUUID())
                .city("Bogotá")
                .tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .timezone("America/Bogota")
                .currency("COP")
                .defaultAppointmentMinutes(30)
                .allowOnlineBooking(true)
                .bookingRequiresConfirmation(true)
                .cancellationMinHours(4)
                .directoryVisible(true)
                .createdAt(LocalDateTime.now())
                .build()).getId();
    }

    @Test
    void employeeInvitation_shouldPersist_withOnlyTheFieldsInviteEmployeeSets() {
        String tokenHash = "a".repeat(64);
        EmployeeInvitation invitation = EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(persistClinic())
                .email("nuevo@clinica.com")
                .role(EmployeeRole.VETERINARIAN)
                .tokenHash(tokenHash)
                // status se omite a propósito: debe salir del @Builder.Default del modelo,
                // no del DEFAULT de la columna (Hibernate insertaría NULL explícito).
                .invitedBy(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        invitationRepositoryPort.save(invitation);

        // La lectura fuerza el flush: sin ella el insert nunca llegaría a Postgres y el test
        // no probaría nada de la base.
        EmployeeInvitation saved = invitationRepositoryPort.findByTokenHash(tokenHash).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(saved.getAcceptedUserId()).isNull();
        assertThat(saved.getDecidedAt()).isNull();
    }

    @Test
    void employeeInvitation_shouldReadBackEveryColumnByTokenHash() {
        String tokenHash = "b".repeat(64);
        UUID invitationId = UUID.randomUUID();
        UUID invitedBy = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7).withNano(0);

        invitationRepositoryPort.save(EmployeeInvitation.builder()
                .id(invitationId)
                .veterinaryId(persistClinic())
                .email("lectura@clinica.com")
                .role(EmployeeRole.ASSISTANT)
                .tokenHash(tokenHash)
                .status(InvitationStatus.PENDING)
                .invitedBy(invitedBy)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now().withNano(0))
                .build());

        Optional<EmployeeInvitation> found = invitationRepositoryPort.findByTokenHash(tokenHash);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(invitationId);
        assertThat(found.get().getEmail()).isEqualTo("lectura@clinica.com");
        assertThat(found.get().getRole()).isEqualTo(EmployeeRole.ASSISTANT);
        assertThat(found.get().getInvitedBy()).isEqualTo(invitedBy);
        assertThat(found.get().getExpiresAt()).isEqualTo(expiresAt);
    }

    /**
     * Consecuencia real de reenviar: {@code ResendInvitationService} sobrescribe {@code token_hash},
     * así que el enlace que ya estaba en el correo del invitado deja de resolver. Se prueba contra la
     * base (y no con un mock del repositorio) porque lo que importa es justamente que la búsqueda por
     * el hash viejo no devuelva nada.
     */
    @Test
    void employeeInvitation_shouldStopResolvingByTheOldTokenHash_afterTheHashIsReplaced() {
        String oldTokenHash = InvitationTokens.hash("token-viejo");
        String newTokenHash = InvitationTokens.hash("token-nuevo");

        EmployeeInvitation invitation = invitationRepositoryPort.save(EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(persistClinic())
                .email("reenvio@clinica.com")
                .role(EmployeeRole.RECEPTIONIST)
                .tokenHash(oldTokenHash)
                .status(InvitationStatus.PENDING)
                .invitedBy(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build());
        assertThat(invitationRepositoryPort.findByTokenHash(oldTokenHash)).isPresent();

        invitationRepositoryPort.save(invitation.toBuilder()
                .tokenHash(newTokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());

        assertThat(invitationRepositoryPort.findByTokenHash(oldTokenHash)).isEmpty();
        assertThat(invitationRepositoryPort.findByTokenHash(newTokenHash))
                .isPresent()
                .get()
                .extracting(EmployeeInvitation::getId)
                .isEqualTo(invitation.getId());
    }

    /**
     * {@code findClinicMembership} es la consulta de la que depende <em>toda</em> la autorización
     * desde la Fase 3 (PASO 0b), y es la única con un join escrito a mano entre dos entidades sin
     * asociación JPA mapeada. Los tests con Mockito no la ejecutan nunca, así que un error en el
     * JPQL o un alias que no case con la proyección solo aparecería en producción: por eso se
     * prueba contra Postgres real.
     */
    @Test
    void findClinicMembership_shouldReturnTheRoleAndClinicStatusInASingleQuery() {
        UUID veterinaryId = persistClinic();
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        employeeRepositoryPort.save(VeterinaryEmployee.builder()
                .id(employeeId)
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());

        Optional<VeterinaryEmployeeRepositoryPort.ClinicMembership> membership =
                employeeRepositoryPort.findClinicMembership(veterinaryId, userId);

        assertThat(membership).isPresent();
        assertThat(membership.get().employeeId()).isEqualTo(employeeId);
        assertThat(membership.get().role()).isEqualTo(EmployeeRole.VETERINARIAN);
        assertThat(membership.get().active()).isTrue();
        assertThat(membership.get().veterinaryStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
    }

    @Test
    void findClinicMembership_shouldReportTheEmployeeAsInactiveAfterBeingRemoved() {
        UUID veterinaryId = persistClinic();
        UUID userId = UUID.randomUUID();
        VeterinaryEmployee employee = employeeRepositoryPort.save(VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(EmployeeRole.ASSISTANT)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());

        employeeRepositoryPort.save(employee.toBuilder()
                .active(false)
                .removedAt(LocalDateTime.now())
                .build());

        Optional<VeterinaryEmployeeRepositoryPort.ClinicMembership> membership =
                employeeRepositoryPort.findClinicMembership(veterinaryId, userId);

        // La fila sigue existiendo (trazabilidad) pero ya no habilita a operar.
        assertThat(membership).isPresent();
        assertThat(membership.get().active()).isFalse();
    }

    @Test
    void findClinicMembership_shouldBeEmpty_whenTheUserNeverWorkedAtTheClinic() {
        UUID veterinaryId = persistClinic();

        assertThat(employeeRepositoryPort.findClinicMembership(veterinaryId, UUID.randomUUID())).isEmpty();
    }

    /**
     * La transferencia de propiedad depende de este {@code UPDATE} condicional y del orden en que se
     * aplica: bajar al OWNER actual y después subir al nuevo. Hacerlo al revés viola el índice único
     * parcial {@code ux_one_owner_per_vet} (un solo OWNER activo por clínica), y eso solo se puede
     * comprobar contra Postgres real — con mocks el orden parece irrelevante.
     */
    @Test
    void claimRoleTransition_shouldSupportTheTransferOrder_demoteTheOwnerThenPromoteTheNewOne() {
        UUID veterinaryId = persistClinic();
        VeterinaryEmployee owner = persistEmployee(veterinaryId, EmployeeRole.OWNER);
        VeterinaryEmployee vet = persistEmployee(veterinaryId, EmployeeRole.VETERINARIAN);

        assertThat(employeeRepositoryPort.claimRoleTransition(
                owner.getId(), EmployeeRole.OWNER, EmployeeRole.ADMIN)).isTrue();
        assertThat(employeeRepositoryPort.claimRoleTransition(
                vet.getId(), EmployeeRole.VETERINARIAN, EmployeeRole.OWNER)).isTrue();

        assertThat(employeeRepositoryPort.findById(owner.getId()))
                .get().extracting(VeterinaryEmployee::getRole).isEqualTo(EmployeeRole.ADMIN);
        assertThat(employeeRepositoryPort.findById(vet.getId()))
                .get().extracting(VeterinaryEmployee::getRole).isEqualTo(EmployeeRole.OWNER);
    }

    @Test
    void claimRoleTransition_shouldReturnFalse_whenTheRoleIsNotTheExpectedOne() {
        UUID veterinaryId = persistClinic();
        VeterinaryEmployee assistant = persistEmployee(veterinaryId, EmployeeRole.ASSISTANT);

        // Es la carrera: otro llegó antes y el rol ya no es el que esperábamos.
        assertThat(employeeRepositoryPort.claimRoleTransition(
                assistant.getId(), EmployeeRole.OWNER, EmployeeRole.ADMIN)).isFalse();
        assertThat(employeeRepositoryPort.findById(assistant.getId()))
                .get().extracting(VeterinaryEmployee::getRole).isEqualTo(EmployeeRole.ASSISTANT);
    }

    @Test
    void claimRoleTransition_shouldReturnFalse_whenTheEmployeeWasRemoved() {
        UUID veterinaryId = persistClinic();
        VeterinaryEmployee removed = employeeRepositoryPort.save(
                persistEmployee(veterinaryId, EmployeeRole.VETERINARIAN).toBuilder()
                        .active(false)
                        .removedAt(LocalDateTime.now())
                        .build());

        assertThat(employeeRepositoryPort.claimRoleTransition(
                removed.getId(), EmployeeRole.VETERINARIAN, EmployeeRole.OWNER)).isFalse();
    }

    private VeterinaryEmployee persistEmployee(UUID veterinaryId, EmployeeRole role) {
        return employeeRepositoryPort.save(VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(UUID.randomUUID())
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    void employeeEmployee_shouldPersistAndReadBackRemovedAt() {
        UUID veterinaryId = persistClinic();
        LocalDateTime removedAt = LocalDateTime.now().withNano(0);
        VeterinaryEmployee saved = employeeRepositoryPort.save(VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(UUID.randomUUID())
                .role(EmployeeRole.RECEPTIONIST)
                .active(false)
                .createdAt(LocalDateTime.now())
                .removedAt(removedAt)
                .build());

        assertThat(employeeRepositoryPort.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(VeterinaryEmployee::getRemovedAt)
                .isEqualTo(removedAt);
    }
}
