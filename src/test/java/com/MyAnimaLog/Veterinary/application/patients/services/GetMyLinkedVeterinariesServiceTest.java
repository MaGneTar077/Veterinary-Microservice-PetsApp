package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.GetMyLinkedVeterinariesResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMyLinkedVeterinariesServiceTest {

    @Mock
    private UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryPatientRepositoryPort patientRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private GetMyLinkedVeterinariesService getMyLinkedVeterinariesService;

    private UUID userId;
    private UUID firstId;
    private UUID secondId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        firstId = UUID.randomUUID();
        secondId = UUID.randomUUID();

        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "ana@correo.com", true, false, Optional.empty()));
    }

    private UserVeterinaryLink link(UUID veterinaryId) {
        return UserVeterinaryLink.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(veterinaryId)
                .status(LinkStatus.LINKED)
                .linkedAt(LocalDateTime.now().minusDays(2))
                .build();
    }

    private Veterinary clinic(UUID id, String name, VeterinaryStatus status) {
        return Veterinary.builder()
                .id(id)
                .name(name)
                .city("Cartagena")
                .phone("3001234567")
                .logoUrl("https://cdn.test/" + name + ".png")
                .tenantId(UUID.randomUUID().toString())
                .status(status)
                .active(status == VeterinaryStatus.ACTIVE)
                .build();
    }

    @Test
    void list_shouldReturnLinkedClinicsWithTheirSharedPetCount() {
        when(linkRepositoryPort.findActiveByUserId(userId))
                .thenReturn(List.of(link(firstId), link(secondId)));
        when(veterinaryRepositoryPort.findByIdIn(anyList())).thenReturn(List.of(
                clinic(firstId, "El Bosque", VeterinaryStatus.ACTIVE),
                clinic(secondId, "San Roque", VeterinaryStatus.ACTIVE)));
        when(patientRepositoryPort.countGrantedByVeterinaryIdAndGrantedBy(firstId, userId)).thenReturn(2L);
        when(patientRepositoryPort.countGrantedByVeterinaryIdAndGrantedBy(secondId, userId)).thenReturn(0L);

        GetMyLinkedVeterinariesResponse response = getMyLinkedVeterinariesService.list();

        assertThat(response.getVeterinaries())
                .extracting(GetMyLinkedVeterinariesResponse.LinkedVeterinary::getName,
                        GetMyLinkedVeterinariesResponse.LinkedVeterinary::getSharedPetsCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("El Bosque", 2L),
                        org.assertj.core.groups.Tuple.tuple("San Roque", 0L));
    }

    @Test
    void list_shouldResolveClinicsInASingleBatch_notOnePerRow() {
        when(linkRepositoryPort.findActiveByUserId(userId))
                .thenReturn(List.of(link(firstId), link(secondId)));
        when(veterinaryRepositoryPort.findByIdIn(List.of(firstId, secondId))).thenReturn(List.of(
                clinic(firstId, "El Bosque", VeterinaryStatus.ACTIVE),
                clinic(secondId, "San Roque", VeterinaryStatus.ACTIVE)));

        getMyLinkedVeterinariesService.list();

        verify(veterinaryRepositoryPort, times(1)).findByIdIn(List.of(firstId, secondId));
    }

    @Test
    void list_shouldIncludeASuspendedClinic_soTheFrontendCanExplainIt() {
        when(linkRepositoryPort.findActiveByUserId(userId)).thenReturn(List.of(link(firstId)));
        when(veterinaryRepositoryPort.findByIdIn(anyList()))
                .thenReturn(List.of(clinic(firstId, "El Bosque", VeterinaryStatus.SUSPENDED)));

        GetMyLinkedVeterinariesResponse response = getMyLinkedVeterinariesService.list();

        assertThat(response.getVeterinaries()).hasSize(1);
        assertThat(response.getVeterinaries().get(0).getStatus()).isEqualTo(VeterinaryStatus.SUSPENDED);
    }

    @Test
    void list_shouldReturnEmpty_whenUserHasNoLinks() {
        when(linkRepositoryPort.findActiveByUserId(userId)).thenReturn(List.of());
        when(veterinaryRepositoryPort.findByIdIn(List.of())).thenReturn(List.of());

        assertThat(getMyLinkedVeterinariesService.list().getVeterinaries()).isEmpty();
    }

    @Test
    void list_shouldSkipALinkWhoseClinicCannotBeResolved() {
        when(linkRepositoryPort.findActiveByUserId(userId))
                .thenReturn(List.of(link(firstId), link(secondId)));
        // La segunda clínica no vuelve del batch: devolver una entrada a medias sería peor.
        when(veterinaryRepositoryPort.findByIdIn(anyList()))
                .thenReturn(List.of(clinic(firstId, "El Bosque", VeterinaryStatus.ACTIVE)));

        GetMyLinkedVeterinariesResponse response = getMyLinkedVeterinariesService.list();

        assertThat(response.getVeterinaries()).hasSize(1);
        assertThat(response.getVeterinaries().get(0).getVeterinaryId()).isEqualTo(firstId);
    }

    @Test
    void list_shouldUseTheUserFromTheToken() {
        when(linkRepositoryPort.findActiveByUserId(userId)).thenReturn(List.of());
        when(veterinaryRepositoryPort.findByIdIn(List.of())).thenReturn(List.of());

        getMyLinkedVeterinariesService.list();

        verify(linkRepositoryPort, times(1)).findActiveByUserId(userId);
    }
}
