package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.patients.enums.ConsentStatus;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.patients.model.VeterinaryPatient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSharedWithVeterinaryServiceTest {

    @Mock
    private VeterinaryPatientRepositoryPort patientRepositoryPort;
    @Mock
    private UserVeterinaryLinkRepositoryPort linkRepositoryPort;

    @InjectMocks
    private GetSharedWithVeterinaryService getSharedWithVeterinaryService;

    private UUID veterinaryId;
    private UUID petId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    private VeterinaryPatient patient(ConsentStatus status) {
        return VeterinaryPatient.builder()
                .id(UUID.randomUUID()).veterinaryId(veterinaryId).petId(petId)
                .consentStatus(status).grantedBy(ownerId).grantedAt(LocalDateTime.now()).build();
    }

    @Test
    void get_shouldBeTrue_whenConsentIsGrantedAndOwnerIsLinked() {
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId))
                .thenReturn(Optional.of(patient(ConsentStatus.GRANTED)));
        when(linkRepositoryPort.findActiveLink(ownerId, veterinaryId))
                .thenReturn(Optional.of(UserVeterinaryLink.builder().id(UUID.randomUUID())
                        .userId(ownerId).veterinaryId(veterinaryId).build()));

        assertThat(getSharedWithVeterinaryService.get(veterinaryId, petId).isShared()).isTrue();
    }

    @Test
    void get_shouldBeFalse_whenConsentWasRevoked() {
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId))
                .thenReturn(Optional.of(patient(ConsentStatus.REVOKED)));

        assertThat(getSharedWithVeterinaryService.get(veterinaryId, petId).isShared()).isFalse();
    }

    @Test
    void get_shouldBeFalse_whenTheOwnerIsNoLongerLinked() {
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId))
                .thenReturn(Optional.of(patient(ConsentStatus.GRANTED)));
        when(linkRepositoryPort.findActiveLink(ownerId, veterinaryId)).thenReturn(Optional.empty());

        assertThat(getSharedWithVeterinaryService.get(veterinaryId, petId).isShared()).isFalse();
    }

    /** Mismo resultado si la clínica/mascota no existen o si el consentimiento es de otra clínica:
     *  la consulta está escopada por {@code veterinaryId}, así que ninguno de los dos casos aparece. */
    @Test
    void get_shouldBeFalse_withStatus200_whenTheClinicOrPetDoNotExist() {
        when(patientRepositoryPort.findByVeterinaryIdAndPetId(veterinaryId, petId)).thenReturn(Optional.empty());

        assertThat(getSharedWithVeterinaryService.get(veterinaryId, petId).isShared()).isFalse();
    }
}
