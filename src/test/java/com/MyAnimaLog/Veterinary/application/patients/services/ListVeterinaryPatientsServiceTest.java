package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.ListVeterinaryPatientsResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.PetsServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ExternalServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListVeterinaryPatientsServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private VeterinaryPatientRepositoryPort patientRepositoryPort;
    @Mock
    private PetsServicePort petsServicePort;

    @InjectMocks
    private ListVeterinaryPatientsService listVeterinaryPatientsService;

    private UUID veterinaryId;
    private UUID petId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        petId = UUID.randomUUID();
    }

    private VeterinaryPatient patient() {
        return VeterinaryPatient.builder()
                .id(UUID.randomUUID())
                .veterinaryId(veterinaryId)
                .petId(petId)
                .grantedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void list_shouldUseLivePetsDataInOneBatchCall() {
        when(patientRepositoryPort.findGrantedByVeterinaryId(veterinaryId, 0, 20))
                .thenReturn(new VeterinaryPatientRepositoryPort.PatientsPage(List.of(patient()), 1));
        when(petsServicePort.getPets(List.of(petId))).thenReturn(Map.of(petId,
                new PetsServicePort.PetInfo(petId, UUID.randomUUID(), "Max", "DOG", "Criollo",
                        "MALE", LocalDate.of(2021, 5, 10), "https://cdn/max.png", null, "CLAIMED")));

        ListVeterinaryPatientsResponse response = listVeterinaryPatientsService.list(veterinaryId, null, null);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getName()).isEqualTo("Max");
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    void list_shouldOmitAPatientPetsCannotResolve() {
        when(patientRepositoryPort.findGrantedByVeterinaryId(veterinaryId, 0, 20))
                .thenReturn(new VeterinaryPatientRepositoryPort.PatientsPage(List.of(patient()), 1));
        when(petsServicePort.getPets(anyList())).thenReturn(Map.of());

        ListVeterinaryPatientsResponse response = listVeterinaryPatientsService.list(veterinaryId, null, null);

        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void list_shouldPropagate503_whenPetsIsDown() {
        when(patientRepositoryPort.findGrantedByVeterinaryId(veterinaryId, 0, 20))
                .thenReturn(new VeterinaryPatientRepositoryPort.PatientsPage(List.of(patient()), 1));
        when(petsServicePort.getPets(anyList()))
                .thenThrow(new ExternalServiceUnavailableException("Pets service lookup failed"));

        assertThatThrownBy(() -> listVeterinaryPatientsService.list(veterinaryId, null, null))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }
}
