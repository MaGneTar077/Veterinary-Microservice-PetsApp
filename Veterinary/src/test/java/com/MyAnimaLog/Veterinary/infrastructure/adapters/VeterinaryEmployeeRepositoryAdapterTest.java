package com.MyAnimaLog.Veterinary.infrastructure.adapters;

import com.MyAnimaLog.Veterinary.domain.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.infrastructure.adapters.VeterinaryEmployeeRepositoryAdapter;
import com.MyAnimaLog.Veterinary.infrastructure.entity.VeterinaryEmployeeEntity;
import com.MyAnimaLog.Veterinary.infrastructure.mapper.VeterinaryEmployeeMapper;
import com.MyAnimaLog.Veterinary.infrastructure.repositories.VeterinaryEmployeeJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeterinaryEmployeeRepositoryAdapterTest {

    @Mock
    private VeterinaryEmployeeJpaRepository jpaRepository;

    @Mock
    private VeterinaryEmployeeMapper mapper;

    @InjectMocks
    private VeterinaryEmployeeRepositoryAdapter adapter;

    private UUID veterinaryId;
    private UUID userId;
    private VeterinaryEmployee domain;
    private VeterinaryEmployeeEntity entity;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        userId = UUID.randomUUID();

        domain = VeterinaryEmployee.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        entity = VeterinaryEmployeeEntity.builder()
                .id(domain.getId())
                .veterinaryId(veterinaryId)
                .userId(userId)
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void save_shouldReturnDomain_whenEntityIsSaved() {
        when(mapper.toEntity(any(VeterinaryEmployee.class))).thenReturn(entity);
        when(jpaRepository.save(any(VeterinaryEmployeeEntity.class))).thenReturn(entity);
        when(mapper.toDomain(any(VeterinaryEmployeeEntity.class))).thenReturn(domain);

        VeterinaryEmployee result = adapter.save(domain);

        assertThat(result).isNotNull();
        assertThat(result.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(result.getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
    }

    @Test
    void save_shouldCallMapperToEntity_once() {
        when(mapper.toEntity(any(VeterinaryEmployee.class))).thenReturn(entity);
        when(jpaRepository.save(any(VeterinaryEmployeeEntity.class))).thenReturn(entity);
        when(mapper.toDomain(any(VeterinaryEmployeeEntity.class))).thenReturn(domain);

        adapter.save(domain);

        verify(mapper, times(1)).toEntity(any(VeterinaryEmployee.class));
    }

    @Test
    void save_shouldCallJpaRepository_once() {
        when(mapper.toEntity(any(VeterinaryEmployee.class))).thenReturn(entity);
        when(jpaRepository.save(any(VeterinaryEmployeeEntity.class))).thenReturn(entity);
        when(mapper.toDomain(any(VeterinaryEmployeeEntity.class))).thenReturn(domain);

        adapter.save(domain);

        verify(jpaRepository, times(1)).save(any(VeterinaryEmployeeEntity.class));
    }

    @Test
    void save_shouldCallMapperToDomain_once() {
        when(mapper.toEntity(any(VeterinaryEmployee.class))).thenReturn(entity);
        when(jpaRepository.save(any(VeterinaryEmployeeEntity.class))).thenReturn(entity);
        when(mapper.toDomain(any(VeterinaryEmployeeEntity.class))).thenReturn(domain);

        adapter.save(domain);

        verify(mapper, times(1)).toDomain(any(VeterinaryEmployeeEntity.class));
    }

    @Test
    void existsByVeterinaryIdAndUserId_shouldReturnTrue_whenExists() {
        when(jpaRepository.existsByVeterinaryIdAndUserId(veterinaryId, userId)).thenReturn(true);

        boolean result = adapter.existsByVeterinaryIdAndUserId(veterinaryId, userId);

        assertThat(result).isTrue();
        verify(jpaRepository, times(1)).existsByVeterinaryIdAndUserId(veterinaryId, userId);
    }

    @Test
    void existsByVeterinaryIdAndUserId_shouldReturnFalse_whenNotExists() {
        when(jpaRepository.existsByVeterinaryIdAndUserId(veterinaryId, userId)).thenReturn(false);

        boolean result = adapter.existsByVeterinaryIdAndUserId(veterinaryId, userId);

        assertThat(result).isFalse();
    }
}