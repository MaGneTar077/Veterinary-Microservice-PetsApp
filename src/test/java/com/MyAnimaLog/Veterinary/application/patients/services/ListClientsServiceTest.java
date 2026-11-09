package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.ListClientsResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.UserVeterinaryLinkRepositoryPort;
import com.MyAnimaLog.Veterinary.application.patients.ports.out.VeterinaryPatientRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.UserServicePort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.patients.enums.LinkStatus;
import com.MyAnimaLog.Veterinary.domain.patients.model.UserVeterinaryLink;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListClientsServiceTest {

    @Mock
    private VeterinaryAuthorizationService authorizationService;
    @Mock
    private UserVeterinaryLinkRepositoryPort linkRepositoryPort;
    @Mock
    private VeterinaryPatientRepositoryPort patientRepositoryPort;
    @Mock
    private UserServicePort userServicePort;

    @InjectMocks
    private ListClientsService listClientsService;

    private UUID veterinaryId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private UserVeterinaryLink link() {
        return UserVeterinaryLink.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .veterinaryId(veterinaryId)
                .status(LinkStatus.LINKED)
                .linkedAt(LocalDateTime.now())
                .ownerName("Snapshot Name")
                .ownerEmail("snapshot@correo.com")
                .build();
    }

    @Test
    void list_shouldPreferLiveUserData_overSnapshot() {
        when(linkRepositoryPort.findActiveByVeterinaryId(veterinaryId, 0, 20))
                .thenReturn(new UserVeterinaryLinkRepositoryPort.ClientsPage(List.of(link()), 1));
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of(
                userId, new UserServicePort.UserInfo(userId, "live@correo.com", true, "Live Name")));
        when(patientRepositoryPort.countGrantedByVeterinaryIdAndGrantedBy(veterinaryId, userId)).thenReturn(3L);

        ListClientsResponse response = listClientsService.list(veterinaryId, null, null);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getName()).isEqualTo("Live Name");
        assertThat(response.getItems().get(0).getEmail()).isEqualTo("live@correo.com");
        assertThat(response.getItems().get(0).getSharedPetsCount()).isEqualTo(3);
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    void list_shouldFallBackToSnapshot_whenUserServiceDoesNotResolve() {
        when(linkRepositoryPort.findActiveByVeterinaryId(veterinaryId, 0, 20))
                .thenReturn(new UserVeterinaryLinkRepositoryPort.ClientsPage(List.of(link()), 1));
        when(userServicePort.getUsers(anyList())).thenReturn(Map.of());
        when(patientRepositoryPort.countGrantedByVeterinaryIdAndGrantedBy(veterinaryId, userId)).thenReturn(0L);

        ListClientsResponse response = listClientsService.list(veterinaryId, null, null);

        assertThat(response.getItems().get(0).getName()).isEqualTo("Snapshot Name");
        assertThat(response.getItems().get(0).getEmail()).isEqualTo("snapshot@correo.com");
    }

    @Test
    void list_shouldCapSizeAt50() {
        when(linkRepositoryPort.findActiveByVeterinaryId(veterinaryId, 0, 50))
                .thenReturn(new UserVeterinaryLinkRepositoryPort.ClientsPage(List.of(), 0));

        listClientsService.list(veterinaryId, 0, 500);

        verify(linkRepositoryPort).findActiveByVeterinaryId(veterinaryId, 0, 50);
    }

    @Test
    void list_shouldRequireClinicalReadBasic() {
        doThrow(new InsufficientPermissionException(Permission.CLINICAL_READ_BASIC))
                .when(authorizationService).require(veterinaryId, Permission.CLINICAL_READ_BASIC);

        assertThatThrownBy(() -> listClientsService.list(veterinaryId, null, null))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(linkRepositoryPort, never()).findActiveByVeterinaryId(any(), anyInt(), anyInt());
    }
}
