package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.dto.EmployeeScheduleResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.GetEmployeeScheduleUseCase;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.EmployeeScheduleRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.schedule.model.EmployeeSchedule;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Devuelve las filas propias del empleado <strong>tal cual están guardadas</strong>, sin completar
 * nada: {@code items} vacío ({@code usesClinicHours = true}) significa "usa el horario de la
 * clínica", no "no atiende nunca". El cálculo de horas efectivas vive en
 * {@code GetScheduleRulesService}, que es lo que consume Calendar.
 */
@Service
@RequiredArgsConstructor
public class GetEmployeeScheduleService implements GetEmployeeScheduleUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final EmployeeScheduleRepositoryPort employeeScheduleRepositoryPort;

    @Override
    public EmployeeScheduleResponse get(UUID veterinaryId, UUID employeeId) {
        authorizationService.requireActiveMember(veterinaryId);
        ScheduleGuards.requireActiveEmployeeOfClinic(employeeRepositoryPort, veterinaryId, employeeId);

        Veterinary clinic = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        List<ScheduleEntryResponse> items = employeeScheduleRepositoryPort
                .findByVeterinaryIdAndEmployeeId(veterinaryId, employeeId).stream()
                .sorted(Comparator.comparing(EmployeeSchedule::getDayOfWeek)
                        .thenComparing(EmployeeSchedule::getStartsAt))
                .map(schedule -> ScheduleEntryResponse.builder()
                        .dayOfWeek(schedule.getDayOfWeek())
                        .opensAt(ScheduleEntries.format(schedule.getStartsAt()))
                        .closesAt(ScheduleEntries.format(schedule.getEndsAt()))
                        .build())
                .toList();

        return EmployeeScheduleResponse.builder()
                .employeeId(employeeId)
                .timezone(clinic.getTimezone())
                .usesClinicHours(items.isEmpty())
                .items(items)
                .build();
    }
}
