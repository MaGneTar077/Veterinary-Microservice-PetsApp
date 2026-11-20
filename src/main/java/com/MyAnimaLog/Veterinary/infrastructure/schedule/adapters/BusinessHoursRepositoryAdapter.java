package com.MyAnimaLog.Veterinary.infrastructure.schedule.adapters;

import com.MyAnimaLog.Veterinary.application.schedule.ports.out.BusinessHoursRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.schedule.model.BusinessHours;
import com.MyAnimaLog.Veterinary.infrastructure.schedule.mapper.BusinessHoursMapper;
import com.MyAnimaLog.Veterinary.infrastructure.schedule.repositories.BusinessHoursJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BusinessHoursRepositoryAdapter implements BusinessHoursRepositoryPort {

    private final BusinessHoursJpaRepository jpaRepository;
    private final BusinessHoursMapper mapper;

    @Override
    public List<BusinessHours> findByVeterinaryId(UUID veterinaryId) {
        return jpaRepository.findByVeterinaryId(veterinaryId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByVeterinaryId(UUID veterinaryId) {
        jpaRepository.deleteByVeterinaryId(veterinaryId);
    }

    @Override
    public List<BusinessHours> saveAll(List<BusinessHours> hours) {
        return jpaRepository.saveAll(hours.stream().map(mapper::toEntity).toList())
                .stream().map(mapper::toDomain).toList();
    }
}
