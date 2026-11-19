package com.MyAnimaLog.Veterinary;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.BusinessHoursRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.EmployeeScheduleRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.ScheduleBlockRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.schedule.model.BusinessHours;
import com.MyAnimaLog.Veterinary.domain.schedule.model.EmployeeSchedule;
import com.MyAnimaLog.Veterinary.domain.schedule.model.ScheduleBlock;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contra Postgres real: el reemplazo de horarios (borrar + insertar) y las dos consultas de solape
 * de bloqueos escritas a mano, que un mock nunca ejecutaría. Las horas son {@code TIME} y los
 * bloqueos {@code TIMESTAMPTZ}, así que también verifica que el mapeo de tipos sobrevive el
 * ida y vuelta.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Fase5SchedulePersistenceIntegrationTest {

    private static final OffsetDateTime FROM = OffsetDateTime.of(2026, 3, 10, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TO = FROM.plusDays(7);

    @Autowired
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Autowired
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Autowired
    private BusinessHoursRepositoryPort businessHoursRepositoryPort;
    @Autowired
    private EmployeeScheduleRepositoryPort employeeScheduleRepositoryPort;
    @Autowired
    private ScheduleBlockRepositoryPort scheduleBlockRepositoryPort;

    private UUID persistEmployee(UUID veterinaryId) {
        return employeeRepositoryPort.save(VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(UUID.randomUUID())
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build()).getId();
    }

    private BusinessHours hours(UUID veterinaryId, int day, String opens, String closes) {
        return BusinessHours.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .dayOfWeek(day)
                .opensAt(LocalTime.parse(opens))
                .closesAt(LocalTime.parse(closes))
                .build();
    }

    private ScheduleBlock block(UUID veterinaryId, UUID employeeId,
                                 OffsetDateTime starts, OffsetDateTime ends) {
        return ScheduleBlock.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .employeeId(employeeId)
                .startsAt(starts)
                .endsAt(ends)
                .reason("Prueba")
                .build();
    }

    @Test
    void businessHours_shouldPersistAndReadBackTheWeek() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);

        businessHoursRepositoryPort.saveAll(List.of(
                hours(veterinaryId, 1, "08:00", "12:00"),
                hours(veterinaryId, 1, "14:00", "18:00")));

        List<BusinessHours> stored = businessHoursRepositoryPort.findByVeterinaryId(veterinaryId);

        assertThat(stored).hasSize(2);
        assertThat(stored).allMatch(row -> row.getDayOfWeek() == 1);
        assertThat(stored).extracting(BusinessHours::getOpensAt)
                .containsExactlyInAnyOrder(LocalTime.of(8, 0), LocalTime.of(14, 0));
    }

    @Test
    void businessHours_replace_shouldLeaveOnlyTheNewWeek() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);
        businessHoursRepositoryPort.saveAll(List.of(hours(veterinaryId, 1, "08:00", "12:00")));

        businessHoursRepositoryPort.deleteByVeterinaryId(veterinaryId);
        businessHoursRepositoryPort.saveAll(List.of(hours(veterinaryId, 5, "09:00", "13:00")));

        List<BusinessHours> stored = businessHoursRepositoryPort.findByVeterinaryId(veterinaryId);

        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getDayOfWeek()).isEqualTo(5);
    }

    @Test
    void employeeSchedules_shouldPersistAndBeDeletablePerEmployee() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);
        UUID employeeId = persistEmployee(veterinaryId);
        UUID otherEmployeeId = persistEmployee(veterinaryId);

        employeeScheduleRepositoryPort.saveAll(List.of(
                EmployeeSchedule.builder().id(UUID.randomUUID()).veterinaryId(veterinaryId)
                        .employeeId(employeeId).dayOfWeek(1)
                        .startsAt(LocalTime.of(9, 0)).endsAt(LocalTime.of(13, 0)).build(),
                EmployeeSchedule.builder().id(UUID.randomUUID()).veterinaryId(veterinaryId)
                        .employeeId(otherEmployeeId).dayOfWeek(2)
                        .startsAt(LocalTime.of(10, 0)).endsAt(LocalTime.of(14, 0)).build()));

        employeeScheduleRepositoryPort.deleteByVeterinaryIdAndEmployeeId(veterinaryId, employeeId);

        assertThat(employeeScheduleRepositoryPort.findByVeterinaryIdAndEmployeeId(veterinaryId, employeeId))
                .isEmpty();
        assertThat(employeeScheduleRepositoryPort.findByVeterinaryId(veterinaryId)).hasSize(1);
        assertThat(employeeScheduleRepositoryPort.findByVeterinaryIdAndEmployeeId(veterinaryId, otherEmployeeId))
                .hasSize(1);
    }

    /**
     * El solape es {@code startsAt < to AND endsAt > from}: entra el que empieza antes de
     * {@code from} y termina dentro, y queda fuera el que empieza exactamente en {@code to}
     * (rango abierto a la derecha).
     */
    @Test
    void scheduleBlocks_findOverlapping_shouldRespectTheHalfOpenRange() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);

        ScheduleBlock startsBeforeEndsInside = scheduleBlockRepositoryPort.save(
                block(veterinaryId, null, FROM.minusDays(2), FROM.plusDays(1)));
        ScheduleBlock fullyInside = scheduleBlockRepositoryPort.save(
                block(veterinaryId, null, FROM.plusDays(2), FROM.plusDays(3)));
        scheduleBlockRepositoryPort.save(block(veterinaryId, null, TO, TO.plusDays(1)));
        scheduleBlockRepositoryPort.save(block(veterinaryId, null, FROM.minusDays(5), FROM.minusDays(4)));

        List<ScheduleBlock> overlapping = scheduleBlockRepositoryPort.findOverlapping(veterinaryId, FROM, TO);

        assertThat(overlapping).extracting(ScheduleBlock::getId)
                .containsExactlyInAnyOrder(startsBeforeEndsInside.getId(), fullyInside.getId());
    }

    @Test
    void scheduleBlocks_findOverlappingForEmployeeOrClinic_shouldAlwaysIncludeWholeClinicBlocks() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);
        UUID employeeId = persistEmployee(veterinaryId);
        UUID otherEmployeeId = persistEmployee(veterinaryId);

        ScheduleBlock wholeClinic = scheduleBlockRepositoryPort.save(
                block(veterinaryId, null, FROM.plusDays(1), FROM.plusDays(2)));
        ScheduleBlock mine = scheduleBlockRepositoryPort.save(
                block(veterinaryId, employeeId, FROM.plusDays(1), FROM.plusDays(2)));
        scheduleBlockRepositoryPort.save(block(veterinaryId, otherEmployeeId, FROM.plusDays(1), FROM.plusDays(2)));

        List<ScheduleBlock> result = scheduleBlockRepositoryPort
                .findOverlappingForEmployeeOrClinic(veterinaryId, employeeId, FROM, TO);

        assertThat(result).extracting(ScheduleBlock::getId)
                .containsExactlyInAnyOrder(wholeClinic.getId(), mine.getId());
    }

    @Test
    void scheduleBlocks_shouldBeScopedByClinicAndDeletable() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);
        UUID otherClinicId = TestClinics.persist(veterinaryRepositoryPort);
        ScheduleBlock saved = scheduleBlockRepositoryPort.save(
                block(veterinaryId, null, FROM, FROM.plusHours(2)));

        assertThat(scheduleBlockRepositoryPort.findByVeterinaryIdAndId(veterinaryId, saved.getId()))
                .isPresent();
        assertThat(scheduleBlockRepositoryPort.findByVeterinaryIdAndId(otherClinicId, saved.getId()))
                .isEmpty();

        scheduleBlockRepositoryPort.deleteById(saved.getId());

        assertThat(scheduleBlockRepositoryPort.findByVeterinaryIdAndId(veterinaryId, saved.getId()))
                .isEmpty();
    }

    @Test
    void scheduleBlocks_shouldPreserveTheInstantAcrossTheRoundTrip() {
        UUID veterinaryId = TestClinics.persist(veterinaryRepositoryPort);
        OffsetDateTime startsAt = OffsetDateTime.of(2026, 3, 11, 13, 30, 0, 0, ZoneOffset.ofHours(-5));
        ScheduleBlock saved = scheduleBlockRepositoryPort.save(
                block(veterinaryId, null, startsAt, startsAt.plusHours(2)));

        ScheduleBlock stored = scheduleBlockRepositoryPort
                .findByVeterinaryIdAndId(veterinaryId, saved.getId()).orElseThrow();

        assertThat(stored.getStartsAt().toInstant()).isEqualTo(startsAt.toInstant());
        assertThat(stored.getEndsAt().toInstant()).isEqualTo(startsAt.plusHours(2).toInstant());
    }
}
