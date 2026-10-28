package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicListItem;
import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicListResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Nit;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;
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
class ListAdminClinicsServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Mock
    private VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;
    @Mock
    private UserServicePort userServicePort;

    @InjectMocks
    private ListAdminClinicsService service;

    @Test
    void list_shouldRequirePlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> service.list(null, null, null, null, null, null, null))
                .isInstanceOf(PlatformAdminRequiredException.class);
    }

    @Test
    void list_shouldApplyDefaultPageAndSize_andCapSizeAt50() {
        when(veterinaryRepositoryPort.findForAdminList(null, null, null, null, null, 0, 20))
                .thenReturn(new VeterinaryRepositoryPort.AdminClinicPage(List.of(), 0));
        when(employeeRepositoryPort.findByVeterinaryIdIn(List.of())).thenReturn(List.of());
        when(subscriptionRepositoryPort.findActiveByVeterinaryIdIn(List.of())).thenReturn(List.of());

        AdminClinicListResponse response = service.list(null, null, null, null, null, null, null);

        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(20);

        when(veterinaryRepositoryPort.findForAdminList(null, null, null, null, null, 0, 50))
                .thenReturn(new VeterinaryRepositoryPort.AdminClinicPage(List.of(), 0));

        AdminClinicListResponse capped = service.list(null, null, null, null, null, 0, 200);
        assertThat(capped.getSize()).isEqualTo(50);
    }

    @Test
    void list_shouldResolveOwnerAndEmployeeCountAndPlan_withoutNPlusOneRepositoryCalls() {
        UUID vetId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Veterinary veterinary = Veterinary.builder()
                .id(vetId).name("Clinica Test").legalName("Clinica Test SAS")
                .nit(Nit.fromPersisted("900123456-1")).city("Bogota").department("Bogota D.C.")
                .status(VeterinaryStatus.ACTIVE).legalRepName("Dueño Legal")
                .build();
        VeterinaryEmployee owner = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vetId).userId(ownerId).role(EmployeeRole.OWNER).active(true).build();
        VeterinaryEmployee staff = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vetId).userId(UUID.randomUUID()).role(EmployeeRole.ASSISTANT).active(true).build();
        VeterinaryEmployee inactiveStaff = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vetId).userId(UUID.randomUUID()).role(EmployeeRole.ASSISTANT).active(false).build();

        when(veterinaryRepositoryPort.findForAdminList(any(), any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new VeterinaryRepositoryPort.AdminClinicPage(List.of(veterinary), 1));
        when(employeeRepositoryPort.findByVeterinaryIdIn(List.of(vetId)))
                .thenReturn(List.of(owner, staff, inactiveStaff));
        when(subscriptionRepositoryPort.findActiveByVeterinaryIdIn(List.of(vetId)))
                .thenReturn(List.of(VeterinarySubscription.builder().veterinaryId(vetId).plan("PRO").active(true).build()));
        when(userServicePort.getUser(ownerId)).thenReturn(new UserServicePort.UserInfo(ownerId, "owner@test.com", true));

        AdminClinicListResponse response = service.list(null, null, null, null, null, null, null);

        AdminClinicListItem item = response.getItems().get(0);
        assertThat(item.getOwnerName()).isEqualTo("Dueño Legal");
        assertThat(item.getOwnerEmail()).isEqualTo("owner@test.com");
        assertThat(item.getEmployeesCount()).isEqualTo(2);
        assertThat(item.getPlan()).isEqualTo("PRO");
    }

    @Test
    void list_shouldOmitOwnerEmail_whenUserServiceFails() {
        UUID vetId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Veterinary veterinary = Veterinary.builder().id(vetId).name("Clinica Test").build();
        VeterinaryEmployee owner = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vetId).userId(ownerId).role(EmployeeRole.OWNER).active(true).build();

        when(veterinaryRepositoryPort.findForAdminList(any(), any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new VeterinaryRepositoryPort.AdminClinicPage(List.of(veterinary), 1));
        when(employeeRepositoryPort.findByVeterinaryIdIn(List.of(vetId))).thenReturn(List.of(owner));
        when(subscriptionRepositoryPort.findActiveByVeterinaryIdIn(List.of(vetId))).thenReturn(List.of());
        when(userServicePort.getUser(ownerId)).thenThrow(new RuntimeException("down"));

        AdminClinicListResponse response = service.list(null, null, null, null, null, null, null);

        assertThat(response.getItems().get(0).getOwnerEmail()).isNull();
    }
}
