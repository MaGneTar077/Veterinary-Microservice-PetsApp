package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.SubmitProfessionalProfileResponse;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitProfessionalProfileServiceTest {

    @Mock
    private ProfessionalProfileSubmissionService professionalProfileSubmissionService;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private SubmitProfessionalProfileService service;

    @Test
    void submit_shouldDelegateToSubmissionService_withCallerUserId() {
        UUID userId = UUID.randomUUID();
        when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "vet@example.com", true, false, Optional.empty()));
        SubmitProfessionalProfileResponse expected = SubmitProfessionalProfileResponse.builder().profileId(UUID.randomUUID()).build();
        when(professionalProfileSubmissionService.submitForUser(userId)).thenReturn(expected);

        SubmitProfessionalProfileResponse response = service.submit();

        assertThat(response).isEqualTo(expected);
        verify(professionalProfileSubmissionService).submitForUser(userId);
    }
}
