package com.MyAnimaLog.Veterinary.application.professional.services;

import com.MyAnimaLog.Veterinary.application.professional.dto.UploadSignatureResponse;
import com.MyAnimaLog.Veterinary.application.professional.ports.out.ProfessionalProfileRepositoryPort;
import com.MyAnimaLog.Veterinary.application.shared.dto.AuthenticatedUser;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Veterinary.application.shared.ports.out.FileStoragePort;
import com.MyAnimaLog.Veterinary.domain.professional.model.ProfessionalProfile;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.FileTooLargeException;
import com.MyAnimaLog.Veterinary.domain.shared.exceptions.InvalidFileTypeException;
import com.MyAnimaLog.Veterinary.infrastructure.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadProfessionalSignatureServiceTest {

    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0};
    private static final byte[] PDF_BYTES = {'%', 'P', 'D', 'F', '-', '1', '.', '4'};

    @Mock
    private ProfessionalProfileRepositoryPort professionalProfileRepositoryPort;

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @Mock
    private FileStoragePort fileStoragePort;

    private UploadProfessionalSignatureService service;
    private UUID userId;
    private ProfessionalProfile profile;

    @BeforeEach
    void setUp() {
        service = new UploadProfessionalSignatureService(professionalProfileRepositoryPort,
                authenticatedUserPort, fileStoragePort, new StorageProperties());
        userId = UUID.randomUUID();
        profile = ProfessionalProfile.builder().id(UUID.randomUUID()).userId(userId).build();

        lenient().when(authenticatedUserPort.current()).thenReturn(
                new AuthenticatedUser(userId, "vet@example.com", true, false, Optional.empty()));
        lenient().when(professionalProfileRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(profile));
        lenient().when(professionalProfileRepositoryPort.save(any(ProfessionalProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void uploadSignature_shouldSucceed_forPng() {
        UploadSignatureResponse response = service.uploadSignature(PNG_BYTES);

        assertThat(response.getHasSignature()).isTrue();
        verify(fileStoragePort, times(1)).uploadPrivate(eq("verification-documents"),
                eq("professionals/" + profile.getId() + "/signature.png"), any(), any());
        verify(professionalProfileRepositoryPort).save(argThat(p ->
                ("professionals/" + profile.getId() + "/signature.png").equals(p.getSignatureUrl())));
    }

    @Test
    void uploadSignature_shouldThrowInvalidFileType_whenNotPng() {
        assertThatThrownBy(() -> service.uploadSignature(PDF_BYTES))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void uploadSignature_shouldThrowFileTooLarge_whenOver1Mb() {
        byte[] big = new byte[1024 * 1024 + 1];
        System.arraycopy(PNG_BYTES, 0, big, 0, PNG_BYTES.length);

        assertThatThrownBy(() -> service.uploadSignature(big))
                .isInstanceOf(FileTooLargeException.class);
    }
}
