package com.MyAnimaLog.Veterinary.application.patients.services;

import com.MyAnimaLog.Veterinary.application.clinic.ports.out.VeterinaryRepositoryPort;
import com.MyAnimaLog.Veterinary.domain.clinic.enums.VeterinaryStatus;
import com.MyAnimaLog.Veterinary.domain.clinic.model.Veterinary;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.InvalidInviteCodeException;
import com.MyAnimaLog.Veterinary.domain.patients.exceptions.LinkCodeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkCodesTest {

    @Mock
    private VeterinaryRepositoryPort veterinaryRepositoryPort;

    private Veterinary activeClinic;

    @BeforeEach
    void setUp() {
        activeClinic = Veterinary.builder()
                .id(UUID.randomUUID())
                .name("Clínica El Bosque")
                .city("Cartagena")
                .tenantId(UUID.randomUUID().toString())
                .status(VeterinaryStatus.ACTIVE)
                .active(true)
                .inviteCode("ABCD2345")
                .build();
    }

    @Test
    void generateUnique_shouldNeverUseAmbiguousCharacters() {
        lenient().when(veterinaryRepositoryPort.existsByInviteCode(anyString())).thenReturn(false);

        // Muchas iteraciones porque el alfabeto se recorre al azar: con una sola, un carácter
        // ambiguo colado en la constante podría no salir nunca.
        IntStream.range(0, 500).forEach(i -> {
            String code = LinkCodes.generateUnique(veterinaryRepositoryPort);
            assertThat(code).hasSize(8).doesNotContainAnyWhitespaces();
            assertThat(code).matches("[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}");
            assertThat(code).doesNotContain("I").doesNotContain("L").doesNotContain("O")
                    .doesNotContain("0").doesNotContain("1");
        });
    }

    @Test
    void generateUnique_shouldRetry_untilTheCodeIsFree() {
        when(veterinaryRepositoryPort.existsByInviteCode(anyString()))
                .thenReturn(true, true, false);

        assertThat(LinkCodes.generateUnique(veterinaryRepositoryPort)).isNotBlank();
    }

    @Test
    void buildLink_shouldAppendCodeWithQuestionMark() {
        assertThat(LinkCodes.buildLink("https://app.test/vincular", "ABCD2345"))
                .isEqualTo("https://app.test/vincular?code=ABCD2345");
    }

    @Test
    void buildLink_shouldAppendCodeWithAmpersand_whenBaseAlreadyHasQueryString() {
        assertThat(LinkCodes.buildLink("https://app.test/vincular?utm=qr", "ABCD2345"))
                .isEqualTo("https://app.test/vincular?utm=qr&code=ABCD2345");
    }

    @Test
    void normalize_shouldUppercaseAndTrim() {
        assertThat(LinkCodes.normalize("  abcd2345 ")).isEqualTo("ABCD2345");
    }

    @Test
    void normalize_shouldReturnNull_whenCodeIsNull() {
        assertThat(LinkCodes.normalize(null)).isNull();
    }

    @Test
    void resolveActiveClinic_shouldReturnClinic_whenCodeMatchesAnActiveOne() {
        when(veterinaryRepositoryPort.findByInviteCode("ABCD2345")).thenReturn(Optional.of(activeClinic));

        assertThat(LinkCodes.resolveActiveClinic(veterinaryRepositoryPort, "ABCD2345"))
                .isEqualTo(activeClinic);
    }

    @Test
    void resolveActiveClinic_shouldAcceptLowercaseAndSpaces() {
        when(veterinaryRepositoryPort.findByInviteCode("ABCD2345")).thenReturn(Optional.of(activeClinic));

        assertThat(LinkCodes.resolveActiveClinic(veterinaryRepositoryPort, " abcd2345 "))
                .isEqualTo(activeClinic);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void resolveActiveClinic_shouldThrowInvalidInviteCode_whenCodeIsBlank(String code) {
        assertThatThrownBy(() -> LinkCodes.resolveActiveClinic(veterinaryRepositoryPort, code))
                .isInstanceOf(InvalidInviteCodeException.class);
    }

    @Test
    void resolveActiveClinic_shouldThrowInvalidInviteCode_whenCodeIsNull() {
        assertThatThrownBy(() -> LinkCodes.resolveActiveClinic(veterinaryRepositoryPort, null))
                .isInstanceOf(InvalidInviteCodeException.class);
    }

    @Test
    void resolveActiveClinic_shouldThrowLinkCodeNotFound_whenCodeDoesNotExist() {
        when(veterinaryRepositoryPort.findByInviteCode("ABCD2345")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> LinkCodes.resolveActiveClinic(veterinaryRepositoryPort, "ABCD2345"))
                .isInstanceOf(LinkCodeNotFoundException.class);
    }

    /**
     * Cualquier estado distinto de ACTIVE da el <strong>mismo</strong> 404 que un código inexistente:
     * si el mensaje o el status distinguieran los casos, se podría ir probando códigos para enumerar
     * qué clínicas existen y en qué estado están.
     */
    @ParameterizedTest
    @EnumSource(value = VeterinaryStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void resolveActiveClinic_shouldThrowLinkCodeNotFound_whenClinicIsNotActive(VeterinaryStatus status) {
        when(veterinaryRepositoryPort.findByInviteCode("ABCD2345"))
                .thenReturn(Optional.of(activeClinic.toBuilder().status(status).build()));

        assertThatThrownBy(() -> LinkCodes.resolveActiveClinic(veterinaryRepositoryPort, "ABCD2345"))
                .isInstanceOf(LinkCodeNotFoundException.class)
                .hasMessage("No active veterinary matches that link code");
    }
}
