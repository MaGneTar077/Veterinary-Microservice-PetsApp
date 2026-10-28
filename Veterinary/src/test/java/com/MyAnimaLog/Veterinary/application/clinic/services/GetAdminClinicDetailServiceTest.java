package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.AdminClinicDetailResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.application.staff.ports.out.VeterinaryEmployeeRepositoryPort;
import com.MyAnimaLog.Veterinary.application.subscription.ports.out.VeterinarySubscriptionRepositoryPort;
import com.MyAnimaLog.Veterinary.application.verification.ports.out.VerificationRequestRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.PlatformAdminRequiredException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.staff.enums.EmployeeRole;
import com.MyAnimaLog.Veterinary.domain.staff.model.VeterinaryEmployee;
import com.MyAnimaLog.Veterinary.domain.subscription.model.VeterinarySubscription;
import com.MyAnimaLog.Veterinary.domain.verification.enums.SubjectType;
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
class GetAdminClinicDetailServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;
    @Mock
    private VeterinaryEmployeeRepositoryPort employeeRepositoryPort;
    @Mock
    private VeterinarySubscriptionRepositoryPort subscriptionRepositoryPort;
    @Mock
    private VerificationRequestRepositoryPort verificationRequestRepositoryPort;
    @Mock
    private UserServicePort userServicePort;

    @InjectMocks
    private GetAdminClinicDetailService service;

    @Test
    void getDetail_shouldRequirePlatformAdmin() {
        doThrow(new PlatformAdminRequiredException()).when(authorizationService).requirePlatformAdmin();

        assertThatThrownBy(() -> service.getDetail(UUID.randomUUID()))
                .isInstanceOf(PlatformAdminRequiredException.class);
    }

    @Test
    void getDetail_shouldThrowVeterinaryNotFound_whenClinicDoesNotExist() {
        UUID vetId = UUID.randomUUID();
        when(veterinaryRepositoryPort.findById(vetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(vetId))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void getDetail_shouldUseUserServiceName_whenAvailable() {
        UUID vetId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Veterinary veterinary = Veterinary.builder()
                .id(vetId).name("Clinica Test").legalRepName("Dueño Legal")
                .suspensionReason(null).build();
        VeterinaryEmployee owner = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vetId).userId(ownerId).role(EmployeeRole.OWNER).active(true).build();

        when(veterinaryRepositoryPort.findById(vetId)).thenReturn(Optional.of(veterinary));
        when(employeeRepositoryPort.findByVeterinaryId(vetId)).thenReturn(List.of(owner));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(vetId))
                .thenReturn(Optional.of(VeterinarySubscription.builder().veterinaryId(vetId).plan("PRO").build()));
        when(userServicePort.getUser(ownerId)).thenReturn(new UserServicePort.UserInfo(ownerId, "owner@test.com", true, "Nombre Real"));
        when(verificationRequestRepositoryPort.findBySubjectTypeAndSubjectId(SubjectType.CLINIC, vetId))
                .thenReturn(List.of());

        AdminClinicDetailResponse response = service.getDetail(vetId);

        assertThat(response.getOwnerName()).isEqualTo("Nombre Real");
        assertThat(response.getLegalRepName()).isEqualTo("Dueño Legal");
        assertThat(response.getOwnerEmail()).isEqualTo("owner@test.com");
        assertThat(response.getPlan()).isEqualTo("PRO");
        assertThat(response.getEmployees()).hasSize(1);
        assertThat(response.getVerificationHistory()).isEmpty();
    }

    @Test
    void getDetail_shouldFallBackOwnerNameToLegalRepName_whenUserServiceHasNoName() {
        UUID vetId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Veterinary veterinary = Veterinary.builder().id(vetId).name("Clinica Test").legalRepName("Dueño Legal").build();
        VeterinaryEmployee owner = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vetId).userId(ownerId).role(EmployeeRole.OWNER).active(true).build();

        when(veterinaryRepositoryPort.findById(vetId)).thenReturn(Optional.of(veterinary));
        when(employeeRepositoryPort.findByVeterinaryId(vetId)).thenReturn(List.of(owner));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(vetId)).thenReturn(Optional.empty());
        when(userServicePort.getUser(ownerId)).thenReturn(new UserServicePort.UserInfo(ownerId, "owner@test.com", true, null));
        when(verificationRequestRepositoryPort.findBySubjectTypeAndSubjectId(SubjectType.CLINIC, vetId)).thenReturn(List.of());

        AdminClinicDetailResponse response = service.getDetail(vetId);

        assertThat(response.getOwnerName()).isEqualTo("Dueño Legal");
        assertThat(response.getOwnerEmail()).isEqualTo("owner@test.com");
    }

    @Test
    void getDetail_shouldFallBackOwnerNameToLegalRepNameAndOmitEmail_whenUserServiceFails() {
        UUID vetId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Veterinary veterinary = Veterinary.builder().id(vetId).name("Clinica Test").legalRepName("Dueño Legal").build();
        VeterinaryEmployee owner = VeterinaryEmployee.builder()
                .id(UUID.randomUUID()).veterinaryId(vetId).userId(ownerId).role(EmployeeRole.OWNER).active(true).build();

        when(veterinaryRepositoryPort.findById(vetId)).thenReturn(Optional.of(veterinary));
        when(employeeRepositoryPort.findByVeterinaryId(vetId)).thenReturn(List.of(owner));
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(vetId)).thenReturn(Optional.empty());
        when(userServicePort.getUser(ownerId)).thenThrow(new RuntimeException("down"));
        when(verificationRequestRepositoryPort.findBySubjectTypeAndSubjectId(SubjectType.CLINIC, vetId)).thenReturn(List.of());

        AdminClinicDetailResponse response = service.getDetail(vetId);

        assertThat(response.getOwnerName()).isEqualTo("Dueño Legal");
        assertThat(response.getOwnerEmail()).isNull();
    }

    @Test
    void getDetail_shouldLeaveOwnerFieldsNull_whenClinicHasNoActiveOwner() {
        UUID vetId = UUID.randomUUID();
        Veterinary veterinary = Veterinary.builder().id(vetId).name("Clinica Test").build();

        when(veterinaryRepositoryPort.findById(vetId)).thenReturn(Optional.of(veterinary));
        when(employeeRepositoryPort.findByVeterinaryId(vetId)).thenReturn(List.of());
        when(subscriptionRepositoryPort.findActiveByVeterinaryId(vetId)).thenReturn(Optional.empty());
        when(verificationRequestRepositoryPort.findBySubjectTypeAndSubjectId(SubjectType.CLINIC, vetId)).thenReturn(List.of());

        AdminClinicDetailResponse response = service.getDetail(vetId);

        assertThat(response.getOwnerName()).isNull();
        assertThat(response.getOwnerEmail()).isNull();
    }
}
