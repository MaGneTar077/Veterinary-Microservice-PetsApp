package com.MyAnimaLog.Veterinary.infrastructure.staff.adapters;

import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.enums.InvitationStatus;
import com.MyAnimaLog.Veterinary.domain.staff.model.EmployeeInvitation;
import com.MyAnimaLog.Veterinary.infrastructure.staff.entity.EmployeeInvitationEntity;
import com.MyAnimaLog.Veterinary.infrastructure.staff.mapper.EmployeeInvitationMapper;
import com.MyAnimaLog.Veterinary.infrastructure.staff.repositories.EmployeeInvitationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Usa el mapper real (no un mock) para que el test también cubra el ida y vuelta
 * entidad ↔ modelo: si alguien agrega una columna y se olvida de mapearla, estos
 * asserts la detectan.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeInvitationRepositoryAdapterTest {

    @Mock
    private EmployeeInvitationJpaRepository jpaRepository;

    private EmployeeInvitationRepositoryAdapter adapter;

    private UUID veterinaryId;
    private EmployeeInvitation domain;
    private EmployeeInvitationEntity entity;

    @BeforeEach
    void setUp() {
        EmployeeInvitationMapper mapper = new EmployeeInvitationMapper();
        adapter = new EmployeeInvitationRepositoryAdapter(jpaRepository, mapper);

        veterinaryId = UUID.randomUUID();
        domain = EmployeeInvitation.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .email("nuevo@clinica.com")
                .role(EmployeeRole.VETERINARIAN)
                .tokenHash("a".repeat(64))
                .status(InvitationStatus.PENDING)
                .invitedBy(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();
        entity = mapper.toEntity(domain);
    }

    @Test
    void save_shouldPersistEveryFieldAndMapBack() {
        when(jpaRepository.save(org.mockito.ArgumentMatchers.any(EmployeeInvitationEntity.class)))
                .thenReturn(entity);

        EmployeeInvitation saved = adapter.save(domain);

        assertThat(saved).usingRecursiveComparison().isEqualTo(domain);
    }

    @Test
    void findByTokenHash_shouldDelegateAndMap() {
        when(jpaRepository.findByTokenHash(domain.getTokenHash())).thenReturn(Optional.of(entity));

        Optional<EmployeeInvitation> found = adapter.findByTokenHash(domain.getTokenHash());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("nuevo@clinica.com");
        verify(jpaRepository).findByTokenHash(domain.getTokenHash());
    }

    @Test
    void findByTokenHash_shouldReturnEmpty_whenNoInvitationMatches() {
        when(jpaRepository.findByTokenHash("desconocido")).thenReturn(Optional.empty());

        assertThat(adapter.findByTokenHash("desconocido")).isEmpty();
    }

    @Test
    void findByVeterinaryIdAndStatus_shouldDelegateToTheOrderedQuery() {
        when(jpaRepository.findByVeterinaryIdAndStatusOrderByCreatedAtDesc(veterinaryId, InvitationStatus.PENDING))
                .thenReturn(List.of(entity));

        List<EmployeeInvitation> invitations =
                adapter.findByVeterinaryIdAndStatus(veterinaryId, InvitationStatus.PENDING);

        assertThat(invitations).hasSize(1);
        assertThat(invitations.get(0).getId()).isEqualTo(domain.getId());
    }

    @Test
    void findByVeterinaryIdAndEmailAndStatus_shouldDelegateAndMap() {
        when(jpaRepository.findByVeterinaryIdAndEmailAndStatus(
                veterinaryId, "nuevo@clinica.com", InvitationStatus.PENDING)).thenReturn(Optional.of(entity));

        Optional<EmployeeInvitation> found = adapter.findByVeterinaryIdAndEmailAndStatus(
                veterinaryId, "nuevo@clinica.com", InvitationStatus.PENDING);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void findByEmailAndStatus_shouldDelegateToTheOrderedQuery() {
        when(jpaRepository.findByEmailAndStatusOrderByCreatedAtDesc("nuevo@clinica.com", InvitationStatus.PENDING))
                .thenReturn(List.of(entity));

        List<EmployeeInvitation> invitations =
                adapter.findByEmailAndStatus("nuevo@clinica.com", InvitationStatus.PENDING);

        assertThat(invitations).hasSize(1);
        assertThat(invitations.get(0).getVeterinaryId()).isEqualTo(veterinaryId);
    }

    @Test
    void findById_shouldDelegateAndMap() {
        when(jpaRepository.findById(domain.getId())).thenReturn(Optional.of(entity));

        assertThat(adapter.findById(domain.getId())).isPresent();
    }
}
