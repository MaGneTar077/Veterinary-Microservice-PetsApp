package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.schedule.dto.CreateScheduleBlockRequest;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleBlockResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.CreateScheduleBlockUseCase;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.ScheduleBlockRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.ClinicContext;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.InvalidScheduleBlockException;
import com.MyAnimaLog.Veterinary.domain.schedule.model.ScheduleBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Dos caminos de autorización: con {@code CLINIC_CONFIGURE} se puede bloquear a cualquiera y a toda
 * la clínica; sin ese permiso, un empleado activo solo puede bloquear <strong>su propio</strong>
 * tiempo.
 *
 * <p>La identidad del empleado que bloquea sale del {@code employeeId} del {@link ClinicContext},
 * que {@code VeterinaryAuthorizationService} resuelve contra la base — nunca del cuerpo de la
 * petición ni del claim del token, que pueden tener hasta 60 min de antigüedad. Por eso mandar el
 * {@code employeeId} de otra persona no alcanza para hacerse pasar por ella.
 */
@Service
@RequiredArgsConstructor
public class CreateScheduleBlockService implements CreateScheduleBlockUseCase {

    private static final int MAX_REASON_LENGTH = 200;

    private final VeterinaryAuthorizationService authorizationService;
    private final VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    private final ScheduleBlockRepositoryPort scheduleBlockRepositoryPort;

    @Override
    public ScheduleBlockResponse create(UUID veterinaryId, CreateScheduleBlockRequest request) {
        ClinicContext caller = authorizationService.requireActiveMember(veterinaryId);

        boolean blockingOwnTime = request.getEmployeeId() != null
                && request.getEmployeeId().equals(caller.employeeId());
        if (!blockingOwnTime) {
            // Incluye el bloqueo de toda la clínica (employeeId == null) y el de otra persona.
            ScheduleGuards.requireClinicConfigure(caller);
        }

        if (request.getEmployeeId() != null) {
            ScheduleGuards.requireActiveEmployeeOfClinic(employeeRepositoryPort, veterinaryId,
                    request.getEmployeeId());
        }
        validate(request);

        ScheduleBlock saved = scheduleBlockRepositoryPort.save(ScheduleBlock.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .employeeId(request.getEmployeeId())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .reason(request.getReason() != null ? request.getReason().trim() : null)
                .build());

        return ScheduleBlockResponse.builder()
                .id(saved.getId())
                .employeeId(saved.getEmployeeId())
                .startsAt(saved.getStartsAt())
                .endsAt(saved.getEndsAt())
                .reason(saved.getReason())
                .build();
    }

    private void validate(CreateScheduleBlockRequest request) {
        if (request.getStartsAt() == null || request.getEndsAt() == null) {
            throw new InvalidScheduleBlockException("startsAt and endsAt are required");
        }
        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new InvalidScheduleBlockException("endsAt must be after startsAt");
        }
        if (request.getReason() != null && request.getReason().trim().length() > MAX_REASON_LENGTH) {
            throw new InvalidScheduleBlockException("reason cannot be longer than " + MAX_REASON_LENGTH
                    + " characters");
        }
    }
}
