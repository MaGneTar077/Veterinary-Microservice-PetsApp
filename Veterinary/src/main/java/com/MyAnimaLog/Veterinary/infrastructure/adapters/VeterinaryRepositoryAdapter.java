package com.MyAnimaLog.Veterinary.infrastructure.adapters;

import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import com.MyAnimaLog.Veterinary.infrastructure.entity.VeterinaryEntity;
import com.MyAnimaLog.Veterinary.infrastructure.mapper.VeterinaryMapper;
import com.MyAnimaLog.Veterinary.infrastructure.repositories.VeterinaryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VeterinaryRepositoryAdapter implements VeterinaryRepositoryPort {

    private final VeterinaryJpaRepository jpaRepository;
    private final VeterinaryMapper mapper;

    @Override
    public Veterinary save(Veterinary veterinary) {
        VeterinaryEntity entity = mapper.toEntity(veterinary);
        VeterinaryEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public Optional<Veterinary> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByInviteCode(String inviteCode) {
        return jpaRepository.existsByInviteCode(inviteCode);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return jpaRepository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public Optional<Veterinary> findByInviteCode(String inviteCode) {
        return jpaRepository.findByInviteCode(inviteCode).map(mapper::toDomain);
    }

    @Override
    public Optional<Veterinary> findByInviteLink(String inviteLink) {
        return jpaRepository.findByInviteLink(inviteLink).map(mapper::toDomain);
    }
}