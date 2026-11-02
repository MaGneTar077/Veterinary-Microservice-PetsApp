package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clínica real para los tests de persistencia. Las tablas con FK a {@code public.veterinary} no
 * aceptan un {@code UUID} inventado, y la FK se valida en el <em>flush</em>, no al llamar
 * {@code save(...)}: un test que solo guarda y nunca lee pasa en verde sin haber ejecutado el
 * {@code INSERT}. Por eso los tests de persistencia crean la clínica con esto y después leen lo
 * guardado (la lectura fuerza el flush).
 *
 * <p>Replica los valores que pone {@code RegisterVeterinaryService} en las columnas
 * {@code NOT NULL DEFAULT ...}: el modelo de dominio no tiene {@code @Builder.Default} para ellas,
 * así que si no se setean Hibernate inserta {@code NULL} explícito y el {@code DEFAULT} de Postgres
 * no se aplica (ver CLAUDE.md, "Base de datos").
 */
final class TestClinics {

    private TestClinics() {
    }

    static UUID persist(VeterinaryRepositoryPort veterinaryRepositoryPort) {
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
}
