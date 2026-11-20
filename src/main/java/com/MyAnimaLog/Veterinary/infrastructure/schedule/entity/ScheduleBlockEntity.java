package com.MyAnimaLog.Veterinary.infrastructure.schedule.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * {@code starts_at}/{@code ends_at} son {@code TIMESTAMPTZ} y se mapean a {@link OffsetDateTime},
 * no a {@code LocalDateTime}: un bloqueo es un instante y perder la zona haría que un festivo se
 * corriera de hora según dónde corra el proceso.
 */
@Entity
@Table(name = "schedule_blocks")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleBlockEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "veterinary_id", nullable = false)
    private UUID veterinaryId;

    /** {@code null} = toda la clínica. */
    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "reason", length = 200)
    private String reason;
}
