package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.GetVeterinaryPatientResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.PetsServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.PatientNotFoundException;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetVeterinaryPatientServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private VeterinaryPatientRepositoryPort patientRepositoryPort;
    @Mock
    private PetsServicePort petsServicePort;

    @InjectMocks
    private GetVeterinaryPatientService getVeterinaryPatientService;

    private UUID veterinaryId;
    private UUID petId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        petId = UUID.randomUUID();
    }

    @Test
    void get_shouldReturnLivePetData_whenGranted() {
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId)).thenReturn(Optional.of(
                VeterinaryPatient.builder().id(UUID.randomUUID()).veterinaryId(veterinaryId).petId(petId)
                        .consentStatus(ConsentStatus.GRANTED).grantedAt(LocalDateTime.now()).build()));
        when(petsServicePort.getPets(java.util.List.of(petId))).thenReturn(Map.of(petId,
                new PetsServicePort.PetInfo(petId, UUID.randomUUID(), "Max", "DOG", null, "MALE",
                        null, null, null, "CLAIMED")));

        GetVeterinaryPatientResponse response = getVeterinaryPatientService.get(veterinaryId, petId);

        assertThat(response.getName()).isEqualTo("Max");
    }

    @Test
    void get_shouldThrow404_whenConsentWasRevoked() {
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId)).thenReturn(Optional.of(
                VeterinaryPatient.builder().id(UUID.randomUUID()).veterinaryId(veterinaryId).petId(petId)
                        .consentStatus(ConsentStatus.REVOKED).build()));

        assertThatThrownBy(() -> getVeterinaryPatientService.get(veterinaryId, petId))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void get_shouldThrow404_whenNeverShared() {
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getVeterinaryPatientService.get(veterinaryId, petId))
                .isInstanceOf(PatientNotFoundException.class);
    }
}
