package com.MyAnimaLog.Veterinary.application.clinic.services;

import com.MyAnimaLog.Veterinary.application.clinic.dto.DeleteInviteCodeResponse;
import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.services.VeterinaryAuthorizationService;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.security.Permission;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InsufficientPermissionException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteInviteCodeServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @Mock
    private VeterinaryAuthorizationService authorizationService;

    @InjectMocks
    private DeleteInviteCodeService deleteInviteCodeService;

    private UUID veterinaryId;
    private Veterinary veterinaryWithInvite;

    @BeforeEach
    void setUp() {
        veterinaryId = UUID.randomUUID();
        veterinaryWithInvite = Veterinary.builder()
                .id(veterinaryId)
                .inviteCode("VET-ABC12345")
                .inviteLink("http://localhost:8082/api/veterinary/join?code=VET-ABC12345")
                .build();

        lenient().when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void deleteInviteCode_shouldClearInviteCodeAndLink() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinaryWithInvite));

        DeleteInviteCodeResponse response = deleteInviteCodeService.deleteInviteCode(veterinaryId);

        assertThat(response.getInviteCode()).isNull();
        assertThat(response.getInviteLink()).isNull();
    }

    @Test
    void deleteInviteCode_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteInviteCodeService.deleteInviteCode(veterinaryId))
                .isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void deleteInviteCode_shouldThrowInsufficientPermissionException_whenCallerLacksClinicConfigure() {
        doThrow(new InsufficientPermissionException(Permission.CLINIC_CONFIGURE))
                .when(authorizationService).require(veterinaryId, Permission.CLINIC_CONFIGURE);

        assertThatThrownBy(() -> deleteInviteCodeService.deleteInviteCode(veterinaryId))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}
