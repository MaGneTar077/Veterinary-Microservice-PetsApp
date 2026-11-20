package com.MyAnimaLog.Veterinary.infrastructure.schedule.adapters;

import com.MyAnimaLog.Veterinary.application.schedule.ports.out.ScheduleBlockRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.schedule.model.ScheduleBlock;
import com.MyAnimaLog.Veterinary.infrastructure.schedule.mapper.ScheduleBlockMapper;
import com.MyAnimaLog.Veterinary.infrastructure.schedule.repositories.ScheduleBlockJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScheduleBlockRepositoryAdapter implements ScheduleBlockRepositoryPort {

    private final ScheduleBlockJpaRepository jpaRepository;
    private final ScheduleBlockMapper mapper;

    @Override
    public ScheduleBlock save(ScheduleBlock block) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(block)));
    }

    @Override
    public Optional<ScheduleBlock> findByVeterinaryIdAndId(UUID veterinaryId, UUID blockId) {
        return jpaRepository.findByVeterinaryIdAndId(veterinaryId, blockId).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID blockId) {
        jpaRepository.deleteById(blockId);
    }

    @Override
    public List<ScheduleBlock> findOverlapping(UUID veterinaryId, OffsetDateTime from, OffsetDateTime to) {
        return jpaRepository.findOverlapping(veterinaryId, from, to).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ScheduleBlock> findOverlappingForEmployeeOrClinic(UUID veterinaryId, UUID employeeId,
                                                                   OffsetDateTime from, OffsetDateTime to) {
        return jpaRepository.findOverlappingForEmployeeOrClinic(veterinaryId, employeeId, from, to)
                .stream().map(mapper::toDomain).toList();
    }
}
