package com.MyAnimaLog.Veterinary.infrastructure.adapters;

import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import com.MyAnimaLog.Veterinary.infrastructure.adapters.VeterinaryRepositoryAdapter;
import com.MyAnimaLog.Veterinary.infrastructure.entity.VeterinaryEntity;
import com.MyAnimaLog.Veterinary.infrastructure.mapper.VeterinaryMapper;
import com.MyAnimaLog.Veterinary.infrastructure.repositories.VeterinaryJpaRepository;
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
class VeterinaryRepositoryAdapterTest {

    @Mock
    private VeterinaryJpaRepository jpaRepository;

    @Mock
    private VeterinaryMapper mapper;

    @InjectMocks
    private VeterinaryRepositoryAdapter veterinaryRepositoryAdapter;

    private Veterinary domain;
    private VeterinaryEntity entity;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();

        domain = Veterinary.builder()
                .id(id)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        entity = VeterinaryEntity.builder()
                .id(id)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void save_shouldReturnDomain_whenEntityIsSaved() {
        when(mapper.toEntity(any(Veterinary.class))).thenReturn(entity);
        when(jpaRepository.save(any(VeterinaryEntity.class))).thenReturn(entity);
        when(mapper.toDomain(any(VeterinaryEntity.class))).thenReturn(domain);

        Veterinary result = veterinaryRepositoryAdapter.save(domain);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Clínica El Bosque");
        assertThat(result.getEmail()).isEqualTo("elbosque@veterinaria.com");
    }

    @Test
    void save_shouldCallMapperToEntity_once() {
        when(mapper.toEntity(any(Veterinary.class))).thenReturn(entity);
        when(jpaRepository.save(any(VeterinaryEntity.class))).thenReturn(entity);
        when(mapper.toDomain(any(VeterinaryEntity.class))).thenReturn(domain);

        veterinaryRepositoryAdapter.save(domain);

        verify(mapper, times(1)).toEntity(any(Veterinary.class));
    }

    @Test
    void save_shouldCallJpaRepository_once() {
        when(mapper.toEntity(any(Veterinary.class))).thenReturn(entity);
        when(jpaRepository.save(any(VeterinaryEntity.class))).thenReturn(entity);
        when(mapper.toDomain(any(VeterinaryEntity.class))).thenReturn(domain);

        veterinaryRepositoryAdapter.save(domain);

        verify(jpaRepository, times(1)).save(any(VeterinaryEntity.class));
    }

    @Test
    void save_shouldCallMapperToDomain_once() {
        when(mapper.toEntity(any(Veterinary.class))).thenReturn(entity);
        when(jpaRepository.save(any(VeterinaryEntity.class))).thenReturn(entity);
        when(mapper.toDomain(any(VeterinaryEntity.class))).thenReturn(domain);

        veterinaryRepositoryAdapter.save(domain);

        verify(mapper, times(1)).toDomain(any(VeterinaryEntity.class));
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenEmailExists() {
        when(jpaRepository.existsByEmail("elbosque@veterinaria.com")).thenReturn(true);

        boolean result = veterinaryRepositoryAdapter.existsByEmail("elbosque@veterinaria.com");

        assertThat(result).isTrue();
        verify(jpaRepository, times(1)).existsByEmail("elbosque@veterinaria.com");
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenEmailDoesNotExist() {
        when(jpaRepository.existsByEmail("otro@email.com")).thenReturn(false);

        boolean result = veterinaryRepositoryAdapter.existsByEmail("otro@email.com");

        assertThat(result).isFalse();
    }

    @Test
    void existsByName_shouldReturnTrue_whenNameExists() {
        when(jpaRepository.existsByName("Clínica El Bosque")).thenReturn(true);

        boolean result = veterinaryRepositoryAdapter.existsByName("Clínica El Bosque");

        assertThat(result).isTrue();
        verify(jpaRepository, times(1)).existsByName("Clínica El Bosque");
    }

    @Test
    void existsByName_shouldReturnFalse_whenNameDoesNotExist() {
        when(jpaRepository.existsByName("Otra Clínica")).thenReturn(false);

        boolean result = veterinaryRepositoryAdapter.existsByName("Otra Clínica");

        assertThat(result).isFalse();
    }
}