package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.EmployeeInvitationRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
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
}
