package com.MyAnimaLog.Veterinary.infrastructure.schedule.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.schedule.entity.ScheduleBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleBlockJpaRepository extends JpaRepository<ScheduleBlockEntity, UUID> {

    Optional<ScheduleBlockEntity> findByVeterinaryIdAndId(UUID veterinaryId, UUID id);

    /**
     * Solape con {@code [from, to)}: {@code startsAt < to AND endsAt > from}. Así, uno que empieza
     * antes de {@code from} y termina dentro del rango cuenta, y uno que empieza exactamente en
     * {@code to} no (el rango es abierto a la derecha).
     */
    @Query("""
            SELECT b FROM ScheduleBlockEntity b
            WHERE b.veterinaryId = :veterinaryId
              AND b.startsAt < :to
              AND b.endsAt > :from
            ORDER BY b.startsAt
            """)
    List<ScheduleBlockEntity> findOverlapping(@Param("veterinaryId") UUID veterinaryId,
                                               @Param("from") OffsetDateTime from,
                                               @Param("to") OffsetDateTime to);

    /**
     * Igual, pero solo los de ese empleado más los de toda la clínica ({@code employeeId IS NULL}),
     * que le aplican a todo el mundo.
     */
    @Query("""
            SELECT b FROM ScheduleBlockEntity b
            WHERE b.veterinaryId = :veterinaryId
              AND (b.employeeId IS NULL OR b.employeeId = :employeeId)
              AND b.startsAt < :to
              AND b.endsAt > :from
            ORDER BY b.startsAt
            """)
    List<ScheduleBlockEntity> findOverlappingForEmployeeOrClinic(@Param("veterinaryId") UUID veterinaryId,
                                                                  @Param("employeeId") UUID employeeId,
                                                                  @Param("from") OffsetDateTime from,
                                                                  @Param("to") OffsetDateTime to);
}
