package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalDetailResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.exceptions.ProfessionalProfileNotFoundException;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAdminProfessionalDetailServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Mock
    private UserServicePort userServicePort;

    @InjectMocks
    private GetAdminProfessionalDetailService service;

    @Test
    void getDetail_shouldRequirePlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> service.getDetail(UUID.randomUUID()))
                .isInstanceOf(PlatformAdminRequiredException.class);
    }

    @Test
    void getDetail_shouldThrowProfessionalProfileNotFound_whenProfileDoesNotExist() {
        UUID profileId = UUID.randomUUID();
        when(professionalProfileRepositoryPort.findById(profileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(profileId))
                .isInstanceOf(ProfessionalProfileNotFoundException.class);
    }

    @Test
    void getDetail_shouldIncludeEmailAndWorkplaces_whenUserServiceSucceeds() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        ProfessionalProfile profile = ProfessionalProfile.builder()
                .id(profileId).userId(userId).fullName("Juan Pérez").build();
        VeterinaryEmployee employee = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vetId).userId(userId).role(EmployeeRole.ASSISTANT).active(true).build();

        when(professionalProfileRepositoryPort.findById(profileId)).thenReturn(Optional.of(profile));
        when(employeeRepositoryPort.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(employee));
        when(veterinaryRepositoryPort.findById(vetId)).thenReturn(Optional.of(Veterinary.builder().id(vetId).name("Clinica Test").build()));
        when(userServicePort.getUser(userId)).thenReturn(new UserServicePort.UserInfo(userId, "juan@test.com", true, "Juan Pérez"));

        AdminProfessionalDetailResponse response = service.getDetail(profileId);

        assertThat(response.getEmail()).isEqualTo("juan@test.com");
        assertThat(response.getWorkplaces()).hasSize(1);
        assertThat(response.getWorkplaces().get(0).getName()).isEqualTo("Clinica Test");
    }

    @Test
    void getDetail_shouldOmitEmail_whenUserServiceFails() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProfessionalProfile profile = ProfessionalProfile.builder().id(profileId).userId(userId).fullName("Juan Pérez").build();

        when(professionalProfileRepositoryPort.findById(profileId)).thenReturn(Optional.of(profile));
        when(employeeRepositoryPort.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());
        when(userServicePort.getUser(userId)).thenThrow(new RuntimeException("down"));

        AdminProfessionalDetailResponse response = service.getDetail(profileId);

        assertThat(response.getEmail()).isNull();
    }
}
