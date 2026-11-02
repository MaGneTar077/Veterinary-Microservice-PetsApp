package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalListItem;
import com.MyAnimaLog.Veterinary.application.professional.dto.AdminProfessionalListResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.professional.enums.LicenseStatus;
import com.MyAnimaLog.Veterinary.domain.professional.enums.Profession;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAdminProfessionalsServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @InjectMocks
    private ListAdminProfessionalsService service;

    @Test
    void list_shouldRequirePlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> service.list(null, null, null, null))
                .isInstanceOf(PlatformAdminRequiredException.class);
    }

    @Test
    void list_shouldApplyDefaultPageAndSize_andCapSizeAt50() {
        when(professionalProfileRepositoryPort.findForAdminList(null, null, 0, 20))
                .thenReturn(new ProfessionalProfileRepositoryPort.AdminProfessionalPage(List.of(), 0));
        when(employeeRepositoryPort.findByUserIdInAndActiveTrue(List.of())).thenReturn(List.of());
        when(veterinaryRepositoryPort.findByIdIn(List.of())).thenReturn(List.of());

        AdminProfessionalListResponse response = service.list(null, null, null, null);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(20);

        when(professionalProfileRepositoryPort.findForAdminList(null, null, 0, 50))
                .thenReturn(new ProfessionalProfileRepositoryPort.AdminProfessionalPage(List.of(), 0));

        AdminProfessionalListResponse capped = service.list(null, null, 0, 200);
        assertThat(capped.getSize()).isEqualTo(50);
    }

    @Test
    void list_shouldResolveWorkplaces_fromBatchedQueriesOnly() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        ProfessionalProfile profile = ProfessionalProfile.builder()
                .id(profileId).userId(userId).fullName("Juan Pérez").licenseNumber("TP-123")
                .profession(Profession.MV).licenseStatus(LicenseStatus.VERIFIED).build();
        VeterinaryEmployee employee = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vetId).userId(userId).role(EmployeeRole.VETERINARIAN).active(true).build();
        Veterinary clinic = Veterinary.builder().id(vetId).name("Clinica Test").build();

        when(professionalProfileRepositoryPort.findForAdminList(any(), any(), eq(0), eq(20)))
                .thenReturn(new ProfessionalProfileRepositoryPort.AdminProfessionalPage(List.of(profile), 1));
        when(employeeRepositoryPort.findByUserIdInAndActiveTrue(List.of(userId))).thenReturn(List.of(employee));
        when(veterinaryRepositoryPort.findByIdIn(List.of(vetId))).thenReturn(List.of(clinic));

        AdminProfessionalListResponse response = service.list(null, null, null, null);

        AdminProfessionalListItem item = response.getItems().get(0);
        assertThat(item.getFullName()).isEqualTo("Juan Pérez");
        assertThat(item.getWorkplaces()).hasSize(1);
        assertThat(item.getWorkplaces().get(0).getName()).isEqualTo("Clinica Test");
        assertThat(item.getWorkplaces().get(0).getRole()).isEqualTo(EmployeeRole.VETERINARIAN);
    }

    @Test
    void list_shouldMakeAtMostOneEmployeeQueryAndOneVeterinaryQuery_regardlessOfRowCount() {
        List<ProfessionalProfile> fiftyProfiles = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> ProfessionalProfile.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                        .fullName("Profile " + i).build())
                .toList();
        List<UUID> userIds = fiftyProfiles.stream().map(ProfessionalProfile::getUserId).toList();

        when(professionalProfileRepositoryPort.findForAdminList(any(), any(), eq(0), eq(50)))
                .thenReturn(new ProfessionalProfileRepositoryPort.AdminProfessionalPage(fiftyProfiles, 50));
        when(employeeRepositoryPort.findByUserIdInAndActiveTrue(userIds)).thenReturn(List.of());
        when(veterinaryRepositoryPort.findByIdIn(List.of())).thenReturn(List.of());

        service.list(null, null, 0, 50);

        org.mockito.Mockito.verify(employeeRepositoryPort, org.mockito.Mockito.times(1)).findByUserIdInAndActiveTrue(userIds);
    }
}
