package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.GetVeterinaryProfileResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Nit;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.ClinicContextRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.TenantMismatchException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetVeterinaryProfileServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private GetVeterinaryProfileService getVeterinaryProfileService;

    private UUID veterinaryId;
    private Veterinary veterinary;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        veterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .nit(Nit.fromPersisted("123456789-6"))
                .timezone("America/Bogota")
                .build();
    }

    @Test
    void getProfile_shouldReturnFullProfile_whenCallerIsMember() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));

        GetVeterinaryProfileResponse response = getVeterinaryProfileService.getProfile(veterinaryId);

        assertThat(response.getId()).isEqualTo(veterinaryId);
        assertThat(response.getName()).isEqualTo("Clínica El Bosque");
        assertThat(response.getStatus()).isEqualTo(VeterinaryStatus.ACTIVE);
        assertThat(response.getNit()).isEqualTo("123456789-6");
        assertThat(response.getTimezone()).isEqualTo("America/Bogota");
    }

    @Test
    void getProfile_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getVeterinaryProfileService.getProfile(veterinaryId))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void getProfile_shouldThrowClinicContextRequiredException_whenCallerHasNoClinicToken() {
        doThrow(new ClinicContextRequiredException()).when(authorizationService).requireMember(veterinaryId);

        assertThatThrownBy(() -> getVeterinaryProfileService.getProfile(veterinaryId))
                .isInstanceOf(ClinicContextRequiredException.class);
    }

    @Test
    void getProfile_shouldThrowTenantMismatchException_whenCallerBelongsToAnotherVeterinary() {
        doThrow(new TenantMismatchException()).when(authorizationService).requireMember(veterinaryId);

        assertThatThrownBy(() -> getVeterinaryProfileService.getProfile(veterinaryId))
                .isInstanceOf(TenantMismatchException.class);
    }
}
