package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.dto.GetScheduleRulesResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.GetScheduleRulesUseCase;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.BusinessHoursRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.EmployeeScheduleRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.ScheduleBlockRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.EmployeeLicense;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.schedule.model.BusinessHours;
import com.MyAnimaLog.Veterinary.domain.schedule.model.EmployeeSchedule;
import com.MyAnimaLog.Veterinary.domain.schedule.model.ScheduleBlock;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lo que Calendar necesita para calcular franjas libres, en una sola llamada
 * (CALENDAR_REQUESTS_TO_VETERINARY.md, pedido 3). Endpoint interno: autenticado con
 * {@code X-Internal-Api-Key} por el filtro de {@code /internal/**}, nunca expuesto por el gateway,
 * así que no hay chequeo de permisos de usuario acá.
 *
 * <p><strong>Regla de horas efectivas</strong> ({@code employeeHours}), solo para empleados
 * <em>activos</em> de la clínica:
 * <ul>
 *   <li>Si el empleado tiene <strong>al menos una fila</strong> en {@code employee_schedules}, se
 *       devuelven exactamente esas filas. Un día sin fila es un día en el que no atiende: no se
 *       completa con el horario de la clínica.</li>
 *   <li>Si <strong>no tiene ninguna fila</strong>, se devuelve una copia de las
 *       {@code businessHours} de la clínica con su {@code employeeId} — "trabaja cuando la clínica
 *       abre".</li>
 *   <li>Si la clínica no tiene {@code businessHours}, un empleado sin filas no genera ninguna hora
 *       (no hay de dónde copiar), y la lista queda vacía para él.</li>
 * </ul>
 * Con el parámetro {@code employeeId} se aplica la misma regla pero solo para esa persona; si no es
 * un empleado activo de esa clínica, {@code 404 EMPLOYEE_NOT_FOUND}.
 *
 * <p>{@code blocks} son los que se solapan con {@code [from, to)} (abierto a la derecha). Los de
 * toda la clínica ({@code employeeId == null}) se incluyen siempre, también cuando se filtra por un
 * empleado: le aplican igual.
 */
@Service
@RequiredArgsConstructor
public class GetScheduleRulesService implements GetScheduleRulesUseCase {

    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final BusinessHoursRepositoryPort businessHoursRepositoryPort;
    private final EmployeeScheduleRepositoryPort employeeScheduleRepositoryPort;
    private final ScheduleBlockRepositoryPort scheduleBlockRepositoryPort;

    @Override
    public GetScheduleRulesResponse get(UUID veterinaryId, OffsetDateTime from, OffsetDateTime to,
                                         UUID employeeId) {
        ScheduleRanges.requireValid(from, to);

        Veterinary clinic = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        List<UUID> activeEmployeeIds = employeeRepositoryPort.findActiveWithLicense(veterinaryId).stream()
                .map(EmployeeLicense::employeeId)
                .toList();
        if (employeeId != null && !activeEmployeeIds.contains(employeeId)) {
            throw new EmployeeNotFoundException();
        }

        List<BusinessHours> businessHours = businessHoursRepositoryPort.findByVeterinaryId(veterinaryId).stream()
                .sorted(Comparator.comparing(BusinessHours::getDayOfWeek).thenComparing(BusinessHours::getOpensAt))
                .toList();

        List<UUID> targets = employeeId != null ? List.of(employeeId) : activeEmployeeIds;

        return GetScheduleRulesResponse.builder()
                .timezone(clinic.getTimezone())
                .businessHours(businessHours.stream()
                        .map(hours -> GetScheduleRulesResponse.BusinessHour.builder()
                                .dayOfWeek(hours.getDayOfWeek())
                                .opensAt(ScheduleEntries.format(hours.getOpensAt()))
                                .closesAt(ScheduleEntries.format(hours.getClosesAt()))
                                .build())
                        .toList())
                .employeeHours(effectiveEmployeeHours(veterinaryId, targets, businessHours))
                .blocks(blocks(veterinaryId, employeeId, from, to))
                .build();
    }

    private List<GetScheduleRulesResponse.EmployeeHour> effectiveEmployeeHours(
            UUID veterinaryId, List<UUID> targets, List<BusinessHours> businessHours) {

        Map<UUID, List<EmployeeSchedule>> ownRowsByEmployee = employeeScheduleRepositoryPort
                .findByVeterinaryId(veterinaryId).stream()
                .collect(Collectors.groupingBy(EmployeeSchedule::getEmployeeId));

        List<GetScheduleRulesResponse.EmployeeHour> result = new ArrayList<>();
        for (UUID target : targets) {
            List<EmployeeSchedule> ownRows = ownRowsByEmployee.getOrDefault(target, List.of());
            if (!ownRows.isEmpty()) {
                ownRows.stream()
                        .sorted(Comparator.comparing(EmployeeSchedule::getDayOfWeek)
                                .thenComparing(EmployeeSchedule::getStartsAt))
                        .forEach(row -> result.add(GetScheduleRulesResponse.EmployeeHour.builder()
                                .employeeId(target)
                                .dayOfWeek(row.getDayOfWeek())
                                .opensAt(ScheduleEntries.format(row.getStartsAt()))
                                .closesAt(ScheduleEntries.format(row.getEndsAt()))
                                .build()));
                continue;
            }
            // Sin filas propias: hereda el horario de la clínica. Si la clínica no tiene horario,
            // este bucle no agrega nada y el empleado queda sin horas.
            businessHours.forEach(hours -> result.add(GetScheduleRulesResponse.EmployeeHour.builder()
                    .employeeId(target)
                    .dayOfWeek(hours.getDayOfWeek())
                    .opensAt(ScheduleEntries.format(hours.getOpensAt()))
                    .closesAt(ScheduleEntries.format(hours.getClosesAt()))
                    .build()));
        }
        return result;
    }

    private List<GetScheduleRulesResponse.Block> blocks(UUID veterinaryId, UUID employeeId,
                                                         OffsetDateTime from, OffsetDateTime to) {
        List<ScheduleBlock> blocks = employeeId != null
                ? scheduleBlockRepositoryPort.findOverlappingForEmployeeOrClinic(veterinaryId, employeeId, from, to)
                : scheduleBlockRepositoryPort.findOverlapping(veterinaryId, from, to);

        return blocks.stream()
                .map(block -> GetScheduleRulesResponse.Block.builder()
                        .employeeId(block.getEmployeeId())
                        .startsAt(block.getStartsAt())
                        .endsAt(block.getEndsAt())
                        .reason(block.getReason())
                        .build())
                .toList();
    }
}
