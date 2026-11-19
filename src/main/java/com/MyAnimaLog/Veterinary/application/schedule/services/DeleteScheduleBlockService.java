package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.schedule.ports.in.DeleteScheduleBlockUseCase;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.ScheduleBlockRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.ScheduleBlockNotFoundException;
import com.MyAnimaLog.Veterinary.domain.schedule.model.ScheduleBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Borra de verdad: un bloqueo es configuración de agenda, no un dato clínico que haya que conservar
 * para trazabilidad (a diferencia de {@code clinic_services}, que se desactiva porque hay citas que
 * lo referencian).
 *
 * <p>Lo puede borrar quien tenga {@code CLINIC_CONFIGURE} o el propio empleado dueño del bloqueo.
 * Un bloqueo de otra clínica es {@code 404}, igual que uno inexistente.
 */
@Service
@RequiredArgsConstructor
public class DeleteScheduleBlockService implements DeleteScheduleBlockUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final ScheduleBlockRepositoryPort scheduleBlockRepositoryPort;

    @Override
    public void delete(UUID veterinaryId, UUID blockId) {
        ClinicContext caller = authorizationService.requireActiveMember(veterinaryId);

        ScheduleBlock block = scheduleBlockRepositoryPort.findByVeterinaryIdAndId(veterinaryId, blockId)
                .orElseThrow(ScheduleBlockNotFoundException::new);

        boolean ownBlock = block.getEmployeeId() != null
                && block.getEmployeeId().equals(caller.employeeId());
        if (!ownBlock) {
            ScheduleGuards.requireClinicConfigure(caller);
        }

        scheduleBlockRepositoryPort.deleteById(blockId);
    }
}
