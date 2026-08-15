package com.MyAnimaLog.Veterinary.application.services;

import com.MyAnimaLog.Veterinary.application.dto.GenerateInviteCodeRequest;
import com.MyAnimaLog.Veterinary.application.dto.GenerateInviteCodeResponse;
import com.MyAnimaLog.Veterinary.application.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.exceptions.VeterinaryNotFoundException;
import com.MyAnimaLog.Veterinary.domain.model.Veterinary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateInviteCodeServiceTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    @InjectMocks
    private GenerateInviteCodeService generateInviteCodeService;

    private UUID veterinaryId;
    private Veterinary veterinary;
    private Veterinary savedVeterinary;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(generateInviteCodeService, "baseUrl", "http://localhost:8082");

        veterinaryId = UUID.randomUUID();

        veterinary = Veterinary.builder()
                .id(veterinaryId)
                .name("Clínica El Bosque")
                .city("Cartagena")
                .phone("3001234567")
                .email("elbosque@veterinaria.com")
                .tenantId(UUID.randomUUID().toString())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        savedVeterinary = veterinary.toBuilder()
                .inviteCode("VET-A3X9K2B7")
                .inviteLink("http://localhost:8082/api/veterinary/join?code=VET-A3X9K2B7")
                .build();
    }

    @Test
    void generateInviteCode_shouldReturnResponse_whenVeterinaryExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(veterinaryRepositoryPort.existsByInviteCode(anyString())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        GenerateInviteCodeResponse response = generateInviteCodeService.generateInviteCode(
                GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build()
        );

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(veterinaryId);
        assertThat(response.getInviteCode()).isNotBlank();
        assertThat(response.getInviteLink()).isNotBlank();
    }

    @Test
    void generateInviteCode_shouldThrowVeterinaryNotFoundException_whenVeterinaryDoesNotExist() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                generateInviteCodeService.generateInviteCode(
                        GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build()
                )
        ).isInstanceOf(VeterinaryNotFoundException.class);
    }

    @Test
    void generateInviteCode_inviteCode_shouldStartWithVET() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(veterinaryRepositoryPort.existsByInviteCode(anyString())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        GenerateInviteCodeResponse response = generateInviteCodeService.generateInviteCode(
                GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build()
        );

        assertThat(response.getInviteCode()).startsWith("VET-");
    }

    @Test
    void generateInviteCode_inviteLink_shouldContainInviteCode() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(veterinaryRepositoryPort.existsByInviteCode(anyString())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        GenerateInviteCodeResponse response = generateInviteCodeService.generateInviteCode(
                GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build()
        );

        assertThat(response.getInviteLink()).contains(response.getInviteCode());
    }

    @Test
    void generateInviteCode_shouldRetryCode_whenCodeAlreadyExists() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(veterinaryRepositoryPort.existsByInviteCode(anyString()))
                .thenReturn(true)   // primera generación colisiona
                .thenReturn(false); // segunda es única
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        GenerateInviteCodeResponse response = generateInviteCodeService.generateInviteCode(
                GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build()
        );

        assertThat(response).isNotNull();
        verify(veterinaryRepositoryPort, times(2)).existsByInviteCode(anyString());
    }

    @Test
    void generateInviteCode_shouldOverwriteExistingCode_whenVeterinaryAlreadyHasOne() {
        Veterinary veterinaryWithCode = veterinary.toBuilder()
                .inviteCode("VET-OLDCODE1")
                .inviteLink("http://localhost:8082/api/veterinary/join?code=VET-OLDCODE1")
                .build();

        Veterinary updatedVeterinary = veterinary.toBuilder()
                .inviteCode("VET-NEWCODE2")
                .inviteLink("http://localhost:8082/api/veterinary/join?code=VET-NEWCODE2")
                .build();

        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinaryWithCode));
        when(veterinaryRepositoryPort.existsByInviteCode(anyString())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(updatedVeterinary);

        GenerateInviteCodeResponse response = generateInviteCodeService.generateInviteCode(
                GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build()
        );

        assertThat(response.getInviteCode()).isNotEqualTo("VET-OLDCODE1");
        verify(veterinaryRepositoryPort, times(1)).save(any(Veterinary.class));
    }

    @Test
    void generateInviteCode_shouldCallSave_once() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.of(veterinary));
        when(veterinaryRepositoryPort.existsByInviteCode(anyString())).thenReturn(false);
        when(veterinaryRepositoryPort.save(any(Veterinary.class))).thenReturn(savedVeterinary);

        generateInviteCodeService.generateInviteCode(
                GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build()
        );

        verify(veterinaryRepositoryPort, times(1)).save(any(Veterinary.class));
    }

    @Test
    void generateInviteCode_shouldNeverCallSave_whenVeterinaryNotFound() {
        when(veterinaryRepositoryPort.findById(veterinaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                generateInviteCodeService.generateInviteCode(
                        GenerateInviteCodeRequest.builder().veterinaryId(veterinaryId).build()
                )
        ).isInstanceOf(VeterinaryNotFoundException.class);

        verify(veterinaryRepositoryPort, never()).save(any());
    }
}