package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.UnlinkUseCase;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnlinkMyVeterinaryServiceTest {

    @Mock
    private UnlinkUseCase unlinkUseCase;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private UnlinkMyVeterinaryService unlinkMyVeterinaryService;

    private UUID userId;
    private UUID veterinaryId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        veterinaryId = UUID.randomUUID();
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "ana@correo.com", true, false, Optional.empty()));
    }

    @Test
    void unlinkMine_shouldDelegateWithTheUserIdFromTheToken() {
        when(unlinkUseCase.unlink(userId, veterinaryId)).thenReturn(UnlinkResponse.builder()
                .userId(userId)
                .veterinaryId(veterinaryId)
                .revokedPetsCount(3)
                .message("User unlinked successfully")
                .build());

        UnlinkResponse response = unlinkMyVeterinaryService.unlinkMine(veterinaryId);

        // No reimplementa nada: toda la lógica (cascada + borrado del contacto) vive en UnlinkUseCase.
        verify(unlinkUseCase, times(1)).unlink(userId, veterinaryId);
        assertThat(response.getRevokedPetsCount()).isEqualTo(3);
        assertThat(response.getUserId()).isEqualTo(userId);
    }
}
