package com.MyAnimaLog.Veterinary.domain.schedule.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Horario propio de un empleado para un día de la semana (ISO: 1 = lunes … 7 = domingo), local a la
 * zona horaria de la clínica.
 *
 * <p><strong>Cero filas significa "usa el horario de la clínica"</strong>, no "no atiende nunca".
 * Si el empleado tiene al menos una fila, esas filas son su horario completo: un día sin fila es un
 * día en el que no atiende. Esa regla la aplica {@code GetScheduleRulesService} al calcular las
 * horas efectivas que consume Calendar.
 *
 * <p>No se exige que caiga dentro del horario de la clínica: la intersección la hace Calendar.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class EmployeeSchedule {
    private UUID id;
    private UUID veterinaryId;
    private UUID employeeId;
    private Integer dayOfWeek;
    private LocalTime startsAt;
    private LocalTime endsAt;
}
