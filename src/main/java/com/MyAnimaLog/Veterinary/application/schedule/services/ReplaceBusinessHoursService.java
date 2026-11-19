package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.schedule.dto.BusinessHoursResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryRequest;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.ReplaceBusinessHoursUseCase;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.BusinessHoursRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.InvalidBusinessHoursException;
import com.MyAnimaLog.Veterinary.domain.schedule.model.BusinessHours;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Reemplaza la <strong>semana completa</strong> en una sola transacción: borra las franjas actuales
 * e inserta las nuevas. El {@code @Transactional} no es decorativo — si falla cualquier inserción,
 * el borrado se revierte y la clínica se queda con el horario anterior en vez de sin horario (que
 * para Calendar significa "no atiende nunca").
 *
 * <p>Una lista vacía es válida y deja la clínica sin horario configurado, de forma explícita.
 */
@Service
@RequiredArgsConstructor
public class ReplaceBusinessHoursService implements ReplaceBusinessHoursUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final VeterinaryRepositoryPort veterinaryRepositoryPort;
    private final BusinessHoursRepositoryPort businessHoursRepositoryPort;

    @Override
    @Transactional
    public BusinessHoursResponse replace(UUID veterinaryId, List<ScheduleEntryRequest> entries) {
        authorizationService.require(veterinaryId, Permission.CLINIC_CONFIGURE);

        Veterinary clinic = veterinaryRepositoryPort.findById(veterinaryId)
                .orElseThrow(VeterinaryNotFoundException::new);

        List<ScheduleEntries.Entry> validated =
                ScheduleEntries.validate(entries, InvalidBusinessHoursException::new);

        businessHoursRepositoryPort.deleteByVeterinaryId(veterinaryId);
        businessHoursRepositoryPort.saveAll(validated.stream()
                .map(entry -> BusinessHours.builder()
                        .id(UUID.randomUUID())
                        .veterinaryId(veterinaryId)
                        .dayOfWeek(entry.dayOfWeek())
                        .opensAt(entry.opensAt())
                        .closesAt(entry.closesAt())
                        .build())
                .toList());

        return BusinessHoursResponse.builder()
                .timezone(clinic.getTimezone())
                .items(ScheduleEntries.toResponse(validated))
                .build();
    }
}
