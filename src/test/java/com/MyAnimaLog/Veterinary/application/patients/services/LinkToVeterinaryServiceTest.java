package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryRequest;
import com.MyAnimaLog.Veterinary.application.patients.dto.LinkToVeterinaryResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.dto.DomainEvent;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.EventPublisherPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.LinkCodeNotFoundException;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.shared.enums.VeterinaryEventType;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkToVeterinaryServiceTest {

    private static final String CODE = "ABCD2345";

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private UserServicePort userServicePort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @InjectMocks
    private LinkToVeterinaryService linkToVeterinaryService;

    private UUID userId;
    private UUID veterinaryId;
    private UUID ownerUserId;
    private Veterinary clinic;
    private LinkToVeterinaryRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();
        ownerUserId = UUID.randomUUID();

        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "ana@correo.com", true, false, Optional.empty()));

        clinic = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .logoUrl("https://cdn.test/logo.png")
                .tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .inviteCode(CODE)
                .build();

        request = LinkToVeterinaryRequest.builder().code(CODE).build();
    }

    private void clinicResolves() {
        when(veterinaryRepositoryPort.findByInviteCode(CODE)).thenReturn(Optional.of(clinic));
    }

    private void echoSave() {
        when(linkRepositoryPort.save(any(UserVeterinaryLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void clinicHasOwner() {
        when(employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.OWNER))
                .thenReturn(Optional.of(VeterinaryEmployee.builder()
                        .id(UUID.randomUUID())
                        .veterinaryId(veterinaryId)
                        .userId(ownerUserId)
                        .role(EmployeeRole.OWNER)
                        .active(true)
                        .build()));
    }

    @Test
    void link_shouldCreateLink_whenCodeIsValidAndUserIsNotLinkedYet() {
        clinicResolves();
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(linkRepositoryPort.findLatestLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of(
                userId, new UserServicePort.UserInfo(userId, "ana@correo.com", true, "Ana Pérez")));
        echoSave();
        clinicHasOwner();

        LinkToVeterinaryResponse response = linkToVeterinaryService.link(request);

        assertThat(response.getVeterinaryId()).isEqualTo(veterinaryId);
        assertThat(response.getVeterinaryName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getLogoUrl()).isEqualTo("https://cdn.test/logo.png");
        assertThat(response.getStatus()).isEqualTo(LinkStatus.LINKED);
        assertThat(response.getLinkedAt()).isNotNull();
        assertThat(response.isAlreadyLinked()).isFalse();
    }

    @Test
    void link_shouldSnapshotTheOwnerContact_fromTokenAndUserService() {
        clinicResolves();
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(linkRepositoryPort.findLatestLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of(
                userId, new UserServicePort.UserInfo(userId, "otro@correo.com", true, "Ana Pérez")));
        echoSave();
        clinicHasOwner();

        linkToVeterinaryService.link(request);

        ArgumentCaptor<UserVeterinaryLink> saved = ArgumentCaptor.forClass(UserVeterinaryLink.class);
        verify(linkRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getOwnerName()).isEqualTo("Ana Pérez");
        // El correo sale del token (fuente confiable y gratis), no de la respuesta del User service.
        assertThat(saved.getValue().getOwnerEmail()).isEqualTo("ana@correo.com");
        // El contrato interno del User service no expone teléfono todavía.
        assertThat(saved.getValue().getOwnerPhone()).isNull();
    }

    @Test
    void link_shouldStillLink_whenUserServiceCannotResolveTheName() {
        clinicResolves();
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(linkRepositoryPort.findLatestLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of());
        echoSave();
        clinicHasOwner();

        LinkToVeterinaryResponse response = linkToVeterinaryService.link(request);

        assertThat(response.isAlreadyLinked()).isFalse();
        ArgumentCaptor<UserVeterinaryLink> saved = ArgumentCaptor.forClass(UserVeterinaryLink.class);
        verify(linkRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getOwnerName()).isNull();
    }

    @Test
    void link_shouldBeIdempotent_whenUserIsAlreadyLinked() {
        clinicResolves();
        UUID existingId = UUID.randomUUID();
        LocalDateTime linkedAt = LocalDateTime.now().minusDays(3);
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.of(
                UserVeterinaryLink.builder()
                        .id(existingId)
                        .userId(userId)
                        .veterinaryId(veterinaryId)
                        .status(LinkStatus.LINKED)
                        .linkedAt(linkedAt)
                        .build()));

        LinkToVeterinaryResponse response = linkToVeterinaryService.link(request);

        // 200 con la vinculación que ya existía, no 409: un doble tap o un reintento del frontend
        // no es un error.
        assertThat(response.isAlreadyLinked()).isTrue();
        assertThat(response.getId()).isEqualTo(existingId);
        assertThat(response.getLinkedAt()).isEqualTo(linkedAt);
        verify(linkRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void link_shouldReactivateTheSameRow_whenUserHadUnlinkedBefore() {
        clinicResolves();
        UUID previousId = UUID.randomUUID();
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(linkRepositoryPort.findLatestLink(userId, veterinaryId)).thenReturn(Optional.of(
                UserVeterinaryLink.builder()
                        .id(previousId)
                        .userId(userId)
                        .veterinaryId(veterinaryId)
                        .status(LinkStatus.UNLINKED)
                        .linkedAt(LocalDateTime.now().minusMonths(2))
                        .unlinkedAt(LocalDateTime.now().minusMonths(1))
                        .ownerName("Ana Pérez")
                        .build()));
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of(
                userId, new UserServicePort.UserInfo(userId, "ana@correo.com", true, "Ana Pérez")));
        echoSave();
        clinicHasOwner();

        LinkToVeterinaryResponse response = linkToVeterinaryService.link(request);

        ArgumentCaptor<UserVeterinaryLink> saved = ArgumentCaptor.forClass(UserVeterinaryLink.class);
        verify(linkRepositoryPort).save(saved.capture());
        // Mismo id: el índice único parcial no admite dos filas LINKED del par, y así no se
        // pierde el historial.
        assertThat(saved.getValue().getId()).isEqualTo(previousId);
        assertThat(saved.getValue().getStatus()).isEqualTo(LinkStatus.LINKED);
        assertThat(saved.getValue().getUnlinkedAt()).isNull();
        // El contacto que se había borrado al desvincularse vuelve a llenarse.
        assertThat(saved.getValue().getOwnerEmail()).isEqualTo("ana@correo.com");
        assertThat(response.getId()).isEqualTo(previousId);
        assertThat(response.isAlreadyLinked()).isFalse();
    }

    @Test
    void link_shouldPublishUserLinkedEvent_toTheClinicOwner() {
        clinicResolves();
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(linkRepositoryPort.findLatestLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of(
                userId, new UserServicePort.UserInfo(userId, "ana@correo.com", true, "Ana Pérez")));
        echoSave();
        clinicHasOwner();

        linkToVeterinaryService.link(request);

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisherPort).publish(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(VeterinaryEventType.USER_LINKED);
        assertThat(event.getValue().userId()).isEqualTo(ownerUserId);
        assertThat(event.getValue().payload())
                .containsEntry("veterinaryName", "Clínica El Bosque")
                .containsEntry("clientUserId", userId)
                .containsEntry("clientName", "Ana Pérez");
        assertThat(event.getValue().metadata().veterinaryId()).isEqualTo(veterinaryId);
    }

    @Test
    void link_shouldStillLink_whenClinicHasNoActiveOwnerToNotify() {
        clinicResolves();
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(linkRepositoryPort.findLatestLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of());
        echoSave();
        when(employeeRepositoryPort.findByVeterinaryIdAndRoleAndActiveTrue(veterinaryId, EmployeeRole.OWNER))
                .thenReturn(Optional.empty());

        assertThat(linkToVeterinaryService.link(request).isAlreadyLinked()).isFalse();

        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void link_shouldThrowLinkCodeNotFound_whenCodeDoesNotExist() {
        when(veterinaryRepositoryPort.findByInviteCode(CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkToVeterinaryService.link(request))
                .isInstanceOf(LinkCodeNotFoundException.class);

        verify(linkRepositoryPort, never()).save(any());
    }

    @Test
    void link_shouldThrowLinkCodeNotFound_whenClinicIsNotActive() {
        when(veterinaryRepositoryPort.findByInviteCode(CODE)).thenReturn(Optional.of(
                clinic.toBuilder().status(VeterinaryStatus.SUSPENDED).active(false).build()));

        assertThatThrownBy(() -> linkToVeterinaryService.link(request))
                .isInstanceOf(LinkCodeNotFoundException.class);

        verify(linkRepositoryPort, never()).save(any());
    }

    @Test
    void link_shouldThrowInvalidInviteCode_whenCodeIsMissing() {
        assertThatThrownBy(() -> linkToVeterinaryService.link(
                LinkToVeterinaryRequest.builder().code("  ").build()))
                .isInstanceOf(InvalidInviteCodeException.class);

        verify(linkRepositoryPort, never()).save(any());
    }

    @Test
    void link_shouldResolveTheUserFromTheToken_notFromTheRequest() {
        clinicResolves();
        when(linkRepositoryPort.findActiveLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(linkRepositoryPort.findLatestLink(userId, veterinaryId)).thenReturn(Optional.empty());
        when(userServicePort.getUsers(List.of(userId))).thenReturn(Map.of());
        echoSave();
        clinicHasOwner();

        linkToVeterinaryService.link(request);

        ArgumentCaptor<UserVeterinaryLink> saved = ArgumentCaptor.forClass(UserVeterinaryLink.class);
        verify(linkRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
    }
}
