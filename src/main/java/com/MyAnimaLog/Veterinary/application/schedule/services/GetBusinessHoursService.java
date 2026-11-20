package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.dto.BusinessHoursResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.GetBusinessHoursUseCase;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.BusinessHoursRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.schedule.model.BusinessHours;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Cualquier miembro activo puede ver el horario; configurarlo exige {@code CLINIC_CONFIGURE}. */
@Service
@RequiredArgsConstructor
public class GetBusinessHoursService implements GetBusinessHoursUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final BusinessHoursRepositoryPort businessHoursRepositoryPort;

    @Override
    public BusinessHoursResponse get(UUID veterinaryId) {
        authorizationService.requireActiveMember(veterinaryId);

        Veterinary clinic = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        List<ScheduleEntryResponse> items = businessHoursRepositoryPort.findByVeterinaryId(veterinaryId).stream()
                .sorted(Comparator.comparing(BusinessHours::getDayOfWeek).thenComparing(BusinessHours::getOpensAt))
                .map(hours -> ScheduleEntryResponse.builder()
                        .dayOfWeek(hours.getDayOfWeek())
                        .opensAt(ScheduleEntries.format(hours.getOpensAt()))
                        .closesAt(ScheduleEntries.format(hours.getClosesAt()))
                        .build())
                .toList();

        return BusinessHoursResponse.builder()
                .timezone(clinic.getTimezone())
                .items(items)
                .build();
    }
}
