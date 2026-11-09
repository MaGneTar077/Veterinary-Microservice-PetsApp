package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.patients.dto.UnlinkResponse;
import com.MyAnimaLog.Veterinary.application.patients.ports.in.UnlinkUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveClientServiceTest {

    @Mock
    private UnlinkUseCase unlinkUseCase;

    @InjectMocks
    private RemoveClientService removeClientService;

    @Test
    void remove_shouldDelegateToUnlinkUseCase_withSameCascadeLogic() {
        UUID veterinaryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(unlinkUseCase.unlink(userId, veterinaryId)).thenReturn(UnlinkResponse.builder()
                .userId(userId).veterinaryId(veterinaryId).revokedPetsCount(2).build());

        UnlinkResponse response = removeClientService.remove(veterinaryId, userId);

        verify(unlinkUseCase, times(1)).unlink(userId, veterinaryId);
        assertThat(response.getRevokedPetsCount()).isEqualTo(2);
    }
}
