package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinariesForPetResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.PetsServicePort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PetNotOwnedException;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetVeterinariesForPetServiceTest {

    @Mock
    private VeterinaryPatientRepositoryPort patientRepositoryPort;

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private PetsServicePort petsServicePort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private GetVeterinariesForPetService getVeterinariesForPetService;

    private UUID userId;
    private UUID petId;
    private UUID firstClinicId;
    private UUID secondClinicId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        petId = UUID.randomUUID();
        firstClinicId = UUID.randomUUID();
        secondClinicId = UUID.randomUUID();

        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "ana@correo.com", true, false, Optional.empty()));
    }

    private VeterinaryPatient share(UUID veterinaryId) {
        return VeterinaryPatient.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .petId(petId)
                .consentStatus(ConsentStatus.GRANTED)
                .grantedBy(userId)
                .grantedAt(LocalDateTime.now().minusDays(5))
                .petName("Max")
                .build();
    }

    private Veterinary clinic(UUID id, String name) {
        return Veterinary.builder()
                .id(id)
                .name(name)
                .city("Cartagena")
                .logoUrl("https://cdn.test/" + name + ".png")
                .tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .build();
    }

    @Test
    void list_shouldReturnTheClinicsThePetIsSharedWith() {
        when(petsServicePort.isOwner(petId, userId)).thenReturn(true);
        when(patientRepositoryPort.findGrantedByPetId(petId))
                .thenReturn(List.of(share(firstClinicId), share(secondClinicId)));
        when(veterinaryRepositoryPort.findByIdIn(anyList())).thenReturn(List.of(
                clinic(firstClinicId, "El Bosque"), clinic(secondClinicId, "San Roque")));

        GetVeterinariesForPetResponse response = getVeterinariesForPetService.list(petId);

        assertThat(response.getPetId()).isEqualTo(petId);
        assertThat(response.getVeterinaries())
                .extracting(GetVeterinariesForPetResponse.SharedWith::getName)
                .containsExactly("El Bosque", "San Roque");
        assertThat(response.getVeterinaries().get(0).getCity()).isEqualTo("Cartagena");
        assertThat(response.getVeterinaries().get(0).getGrantedAt()).isNotNull();
    }

    @Test
    void list_shouldResolveClinicsInASingleBatch() {
        when(petsServicePort.isOwner(petId, userId)).thenReturn(true);
        when(patientRepositoryPort.findGrantedByPetId(petId))
                .thenReturn(List.of(share(firstClinicId), share(secondClinicId)));
        when(veterinaryRepositoryPort.findByIdIn(List.of(firstClinicId, secondClinicId)))
                .thenReturn(List.of(clinic(firstClinicId, "El Bosque"), clinic(secondClinicId, "San Roque")));

        getVeterinariesForPetService.list(petId);

        verify(veterinaryRepositoryPort, times(1)).findByIdIn(List.of(firstClinicId, secondClinicId));
    }

    @Test
    void list_shouldReturnEmpty_whenThePetIsNotSharedWithAnyone() {
        when(petsServicePort.isOwner(petId, userId)).thenReturn(true);
        when(patientRepositoryPort.findGrantedByPetId(petId)).thenReturn(List.of());
        when(veterinaryRepositoryPort.findByIdIn(List.of())).thenReturn(List.of());

        assertThat(getVeterinariesForPetService.list(petId).getVeterinaries()).isEmpty();
    }

    @Test
    void list_shouldThrowPetNotOwned_whenCallerIsNotTheOwner() {
        when(petsServicePort.isOwner(petId, userId)).thenReturn(false);

        // Qué clínicas ven una mascota no se le puede mostrar a quien no es su dueño: un EDITOR o
        // VIEWER de Pets tampoco pasa este chequeo.
        assertThatThrownBy(() -> getVeterinariesForPetService.list(petId))
                .isInstanceOf(PetNotOwnedException.class);

        verify(patientRepositoryPort, never()).findGrantedByPetId(any());
    }

    @Test
    void list_shouldPropagate503_whenPetsIsDown() {
        when(petsServicePort.isOwner(petId, userId))
                .thenThrow(new ExternalServiceUnavailableException("Pets service lookup failed"));

        assertThatThrownBy(() -> getVeterinariesForPetService.list(petId))
                .isInstanceOf(ExternalServiceUnavailableException.class);

        verify(patientRepositoryPort, never()).findGrantedByPetId(any());
    }

    @Test
    void list_shouldSkipAShareWhoseClinicCannotBeResolved() {
        when(petsServicePort.isOwner(petId, userId)).thenReturn(true);
        when(patientRepositoryPort.findGrantedByPetId(petId))
                .thenReturn(List.of(share(firstClinicId), share(secondClinicId)));
        when(veterinaryRepositoryPort.findByIdIn(anyList()))
                .thenReturn(List.of(clinic(firstClinicId, "El Bosque")));

        GetVeterinariesForPetResponse response = getVeterinariesForPetService.list(petId);

        assertThat(response.getVeterinaries()).hasSize(1);
        assertThat(response.getVeterinaries().get(0).getVeterinaryId()).isEqualTo(firstClinicId);
    }

    @Test
    void list_shouldUseTheUserFromTheToken() {
        when(petsServicePort.isOwner(petId, userId)).thenReturn(true);
        when(patientRepositoryPort.findGrantedByPetId(petId)).thenReturn(List.of());
        when(veterinaryRepositoryPort.findByIdIn(List.of())).thenReturn(List.of());

        getVeterinariesForPetService.list(petId);

        verify(petsServicePort, times(1)).isOwner(petId, userId);
    }
}
