package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.dto.EmployeeScheduleResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryRequest;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.ReplaceEmployeeScheduleUseCase;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.EmployeeScheduleRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.InvalidEmployeeScheduleException;
import com.MyAnimaLog.Veterinary.domain.schedule.model.EmployeeSchedule;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Reemplaza el horario propio del empleado en una sola transacción, igual que
 * {@link ReplaceBusinessHoursService}. Una lista vacía borra sus filas y lo devuelve al horario de
 * la clínica (no lo deja sin atender).
 *
 * <p><strong>No</strong> se exige que las franjas caigan dentro del horario de la clínica: la
 * intersección la hace Calendar al calcular disponibilidad. Así, cambiar el horario de la clínica
 * nunca invalida retroactivamente el de una persona.
 */
@Service
@RequiredArgsConstructor
public class ReplaceEmployeeScheduleService implements ReplaceEmployeeScheduleUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final EmployeeScheduleRepositoryPort employeeScheduleRepositoryPort;

    @Override
    @Transactional
    public EmployeeScheduleResponse replace(UUID veterinaryId, UUID employeeId,
                                             List<ScheduleEntryRequest> entries) {
        authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE);
        ScheduleGuards.requireActiveEmployeeOfClinic(employeeRepositoryPort, veterinaryId, employeeId);

        Veterinary clinic = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        List<ScheduleEntries.Entry> validated =
                ScheduleEntries.validate(entries, InvalidEmployeeScheduleException::new);

        employeeScheduleRepositoryPort.deleteByVeterinaryIdAndEmployeeId(veterinaryId, employeeId);
        employeeScheduleRepositoryPort.saveAll(validated.stream()
                .map(entry -> EmployeeSchedule.builder()
                        .id(UUID.randomUUID())
                        .veterinaryId(veterinaryId)
                        .employeeId(employeeId)
                        .dayOfWeek(entry.dayOfWeek())
                        .startsAt(entry.opensAt())
                        .endsAt(entry.closesAt())
                        .build())
                .toList());

        return EmployeeScheduleResponse.builder()
                .employeeId(employeeId)
                .timezone(clinic.getTimezone())
                .usesClinicHours(validated.isEmpty())
                .items(ScheduleEntries.toResponse(validated))
                .build();
    }
}
