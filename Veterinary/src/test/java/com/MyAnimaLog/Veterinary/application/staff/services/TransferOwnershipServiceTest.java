package com.MyAnimaLog.Veterinary.application.staff.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.dto.TransferOwnershipRequest;
import com.MyAnimaLog.Veterinary.application.staff.dto.TransferOwnershipResponse;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort.ClinicMembership;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.NotVeterinaryOwnerException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.EmployeeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.InvalidOwnershipTransferException;
import com.MyAnimaLog.Veterinary.domain.staff.exceptions.OwnershipTransferConflictException;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferOwnershipServiceTest {

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private TransferOwnershipService transferOwnershipService;

    private UUID veterinaryId;
    private UUID currentOwnerEmployeeId;
    private UUID currentOwnerUserId;
    private UUID newOwnerEmployeeId;
    private UUID newOwnerUserId;
    private VeterinaryEmployee newOwner;
    private TransferOwnershipRequest request;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        currentOwnerEmployeeId = UUID.randomUUID();
        currentOwnerUserId = UUID.randomUUID();
        newOwnerEmployeeId = UUID.randomUUID();
        newOwnerUserId = UUID.randomUUID();

        newOwner = VeterinaryEmployee.builder()
                .id(newOwnerEmployeeId)
                .veterinaryId(veterinaryId)
                .userId(newOwnerUserId)
                .role(EmployeeRole.VETERINARIAN)
                .active(true)
                .createdAt(LocalDateTime.now().minusMonths(4))
                .build();

        request = TransferOwnershipRequest.builder().newOwnerEmployeeId(newOwnerEmployeeId).build();

        when(authorizationService.requireVeterinaryOwner(veterinaryId)).thenReturn(
                new ClinicMembership(currentOwnerEmployeeId, EmployeeRole.OWNER, true, VeterinaryStatus.ACTIVE));
        when(authorizationService.currentClinicId()).thenReturn(Optional.of(veterinaryId));
        when(employeeRepositoryPort.findById(newOwnerEmployeeId)).thenReturn(Optional.of(newOwner));
        when(employeeRepositoryPort.findById(currentOwnerEmployeeId)).thenReturn(Optional.of(
                VeterinaryEmployee.builder()
                        .id(currentOwnerEmployeeId)
                        .veterinaryId(veterinaryId)
                        .userId(currentOwnerUserId)
                        .role(EmployeeRole.ADMIN)
                        .active(true)
                        .build()));
        when(employeeRepositoryPort.claimRoleTransition(any(UUID.class), any(), any())).thenReturn(true);
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .status(VeterinaryStatus.ACTIVE)
                .build()));
    }

    @Test
    void transfer_shouldDemoteTheCurrentOwnerBeforePromotingTheNewOne() {
        transferOwnershipService.transfer(veterinaryId, request);

        // El orden es obligatorio: con el índice ux_one_owner_per_vet no pueden existir dos OWNER
        // activos a la vez, así que promover primero fallaría.
        InOrder order = inOrder(employeeRepositoryPort);
        order.verify(employeeRepositoryPort).claimRoleTransition(
                currentOwnerEmployeeId, EmployeeRole.OWNER, EmployeeRole.ADMIN);
        order.verify(employeeRepositoryPort).claimRoleTransition(
                newOwnerEmployeeId, EmployeeRole.VETERINARIAN, EmployeeRole.OWNER);
    }

    @Test
    void transfer_shouldReturnBothPartiesWithTheirNewRoles() {
        TransferOwnershipResponse response = transferOwnershipService.transfer(veterinaryId, request);

        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getPreviousOwner().getEmployeeId()).isEqualTo(currentOwnerEmployeeId);
        assertThat(response.getPreviousOwner().getRole()).isEqualTo(EmployeeRole.ADMIN);
        assertThat(response.getNewOwner().getEmployeeId()).isEqualTo(newOwnerEmployeeId);
        assertThat(response.getNewOwner().getRole()).isEqualTo(EmployeeRole.OWNER);
    }

    @Test
    void transfer_shouldNeverChangeTheLegalRepresentative() {
        TransferOwnershipResponse response = transferOwnershipService.transfer(veterinaryId, request);

        assertThat(response.isLegalRepresentativeChanged()).isFalse();
        // No toca la clínica: solo la lee para el nombre que va en el evento.
        verify(veterinaryRepositoryPort, never()).save(any());
    }

    @Test
    void transfer_shouldRequireBeingTheOwner() {
        doThrow(new NotVeterinaryOwnerException())
                .when(authorizationService).requireVeterinaryOwner(veterinaryId);

        assertThatThrownBy(() -> transferOwnershipService.transfer(veterinaryId, request))
                .isInstanceOf(NotVeterinaryOwnerException.class);

        verify(employeeRepositoryPort, never()).claimRoleTransition(any(), any(), any());
    }

    @Test
    void transfer_shouldThrowValidationError_whenTheNewOwnerIsMissing() {
        assertThatThrownBy(() -> transferOwnershipService.transfer(veterinaryId,
                TransferOwnershipRequest.builder().build()))
                .isInstanceOf(InvalidOwnershipTransferException.class);

        verify(employeeRepositoryPort, never()).claimRoleTransition(any(), any(), any());
    }

    @Test
    void transfer_shouldThrowValidationError_whenTheTargetIsAlreadyTheOwner() {
        assertThatThrownBy(() -> transferOwnershipService.transfer(veterinaryId,
                TransferOwnershipRequest.builder().newOwnerEmployeeId(currentOwnerEmployeeId).build()))
                .isInstanceOf(InvalidOwnershipTransferException.class);

        verify(employeeRepositoryPort, never()).claimRoleTransition(any(), any(), any());
    }

    @Test
    void transfer_shouldThrowValidationError_whenTheTargetIsARemovedEmployee() {
        when(employeeRepositoryPort.findById(newOwnerEmployeeId))
                .thenReturn(Optional.of(newOwner.toBuilder().active(false).build()));

        assertThatThrownBy(() -> transferOwnershipService.transfer(veterinaryId, request))
                .isInstanceOf(InvalidOwnershipTransferException.class);

        verify(employeeRepositoryPort, never()).claimRoleTransition(any(), any(), any());
    }

    @Test
    void transfer_shouldThrowEmployeeNotFound_whenTheTargetDoesNotExist() {
        when(employeeRepositoryPort.findById(newOwnerEmployeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferOwnershipService.transfer(veterinaryId, request))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void transfer_shouldThrowEmployeeNotFound_whenTheTargetBelongsToAnotherClinic() {
        when(employeeRepositoryPort.findById(newOwnerEmployeeId))
                .thenReturn(Optional.of(newOwner.toBuilder().veterinaryId(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> transferOwnershipService.transfer(veterinaryId, request))
                .isInstanceOf(EmployeeNotFoundException.class);

        verify(employeeRepositoryPort, never()).claimRoleTransition(any(), any(), any());
    }

    // ---------- Carrera: dos transferencias simultáneas ----------

    @Test
    void transfer_shouldThrowConflict_whenAnotherTransferAlreadyDemotedTheOwner() {
        // La otra transferencia ganó: el UPDATE condicional del paso 1 no encuentra OWNER.
        when(employeeRepositoryPort.claimRoleTransition(
                currentOwnerEmployeeId, EmployeeRole.OWNER, EmployeeRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> transferOwnershipService.transfer(veterinaryId, request))
                .isInstanceOf(OwnershipTransferConflictException.class);

        // No se intenta promover a nadie: la clínica no puede terminar con dos OWNER.
        verify(employeeRepositoryPort, never()).claimRoleTransition(
                newOwnerEmployeeId, EmployeeRole.VETERINARIAN, EmployeeRole.OWNER);
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void transfer_shouldThrowConflict_whenTheTargetRoleChangedBetweenTheTwoSteps() {
        // El paso 1 pasó, pero al destino le cambiaron el rol en el medio: el paso 2 no actualiza
        // nada y la excepción revierte la transacción completa (el paso 1 incluido), así que la
        // clínica no queda sin OWNER.
        when(employeeRepositoryPort.claimRoleTransition(
                newOwnerEmployeeId, EmployeeRole.VETERINARIAN, EmployeeRole.OWNER)).thenReturn(false);

        assertThatThrownBy(() -> transferOwnershipService.transfer(veterinaryId, request))
                .isInstanceOf(OwnershipTransferConflictException.class);

        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void transfer_shouldUseTheTargetCurrentRoleAsTheExpectedValue() {
        when(employeeRepositoryPort.findById(newOwnerEmployeeId))
                .thenReturn(Optional.of(newOwner.toBuilder().role(EmployeeRole.ADMIN).build()));

        transferOwnershipService.transfer(veterinaryId, request);

        verify(employeeRepositoryPort).claimRoleTransition(
                newOwnerEmployeeId, EmployeeRole.ADMIN, EmployeeRole.OWNER);
    }

    // ---------- Eventos ----------

    @Test
    void transfer_shouldPublishOwnershipTransferredToBothParties() {
        transferOwnershipService.transfer(veterinaryId, request);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort, org.mockito.Mockito.times(2)).publish(captor.capture());
        List<DomainEvent> events = captor.getAllValues();

        assertThat(events).allSatisfy(event -> {
            assertThat(event.eventType()).isEqualTo(VeterinaryEventType.OWNERSHIP_TRANSFERRED);
            assertThat(event.metadata().veterinaryId()).isEqualTo(veterinaryId);
            assertThat(event.payload()).containsEntry("veterinaryName", "Clínica El Bosque");
            assertThat(event.payload()).containsEntry("previousOwnerEmployeeId", currentOwnerEmployeeId);
            assertThat(event.payload()).containsEntry("newOwnerEmployeeId", newOwnerEmployeeId);
        });
        assertThat(events).extracting(DomainEvent::userId)
                .containsExactlyInAnyOrder(newOwnerUserId, currentOwnerUserId);
        assertThat(events).filteredOn(event -> event.userId().equals(newOwnerUserId))
                .singleElement()
                .satisfies(event -> assertThat(event.payload()).containsEntry("recipientIsNewOwner", true));
        assertThat(events).filteredOn(event -> event.userId().equals(currentOwnerUserId))
                .singleElement()
                .satisfies(event -> assertThat(event.payload()).containsEntry("recipientIsNewOwner", false));
    }
}
