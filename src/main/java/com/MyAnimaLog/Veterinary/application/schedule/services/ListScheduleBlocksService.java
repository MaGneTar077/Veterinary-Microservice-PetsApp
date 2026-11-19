package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.schedule.dto.ListScheduleBlocksResponse;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleBlockResponse;
import com.MyAnimaLog.Veterinary.application.schedule.ports.in.ListScheduleBlocksUseCase;
import com.MyAnimaLog.Veterinary.application.schedule.ports.out.ScheduleBlockRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Bloqueos que se solapan con {@code [from, to)}; rango obligatorio y de 92 días como máximo. */
@Service
@RequiredArgsConstructor
public class ListScheduleBlocksService implements ListScheduleBlocksUseCase {

    private final VeterinaryAuthorizationService authorizationService;
    private final ScheduleBlockRepositoryPort scheduleBlockRepositoryPort;

    @Override
    public ListScheduleBlocksResponse list(UUID veterinaryId, OffsetDateTime from, OffsetDateTime to) {
        authorizationService.requireActiveMember(veterinaryId);
        ScheduleRanges.requireValid(from, to);

        return ListScheduleBlocksResponse.builder()
                .items(scheduleBlockRepositoryPort.findOverlapping(veterinaryId, from, to).stream()
                        .map(block -> ScheduleBlockResponse.builder()
                                .id(block.getId())
                                .employeeId(block.getEmployeeId())
                                .startsAt(block.getStartsAt())
                                .endsAt(block.getEndsAt())
                                .reason(block.getReason())
                                .build())
                        .toList())
                .build();
    }
}
