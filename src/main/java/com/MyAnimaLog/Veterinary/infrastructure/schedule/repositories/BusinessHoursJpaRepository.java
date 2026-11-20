package com.MyAnimaLog.Veterinary.infrastructure.schedule.repositories;

import com.MyAnimaLog.Veterinary.infrastructure.schedule.entity.BusinessHoursEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessHoursJpaRepository extends JpaRepository<BusinessHoursEntity, UUID> {

    List<BusinessHoursEntity> findByVeterinaryId(UUID veterinaryId);

    void deleteByVeterinaryId(UUID veterinaryId);
}
