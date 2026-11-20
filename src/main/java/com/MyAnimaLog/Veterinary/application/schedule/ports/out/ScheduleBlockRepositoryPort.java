package com.MyAnimaLog.Veterinary.application.schedule.ports.out;

import com.MyAnimaLog.Veterinary.domain.schedule.model.ScheduleBlock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleBlockRepositoryPort {

    ScheduleBlock save(ScheduleBlock block);

    /** Escopado por clínica: un id de otra clínica no debe poder resolverse desde acá. */
    Optional<ScheduleBlock> findByVeterinaryIdAndId(UUID veterinaryId, UUID blockId);

    void deleteById(UUID blockId);

    /**
     * Bloques que se solapan con {@code [from, to)} — abierto a la derecha: uno que empieza
     * exactamente en {@code to} no cuenta, y uno que empieza antes de {@code from} pero termina
     * dentro sí.
     */
    List<ScheduleBlock> findOverlapping(UUID veterinaryId, OffsetDateTime from, OffsetDateTime to);

    /**
     * Igual que {@link #findOverlapping}, pero solo los de ese empleado más los de toda la clínica
     * ({@code employee_id IS NULL}), que aplican a todo el mundo.
     */
    List<ScheduleBlock> findOverlappingForEmployeeOrClinic(UUID veterinaryId, UUID employeeId,
                                                            OffsetDateTime from, OffsetDateTime to);
}
