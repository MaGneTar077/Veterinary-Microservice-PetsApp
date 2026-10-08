package com.MyAnimaLog.Veterinary.domain.clinic.model;

import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidNitException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NitTest {

    @Test
    void of_shouldReturnNit_whenCheckDigitIsCorrect() {
        Nit nit = Nit.of("123456789-6");

        assertThat(nit.value()).isEqualTo("123456789-6");
    }

    @Test
    void of_shouldAcceptAnotherValidCheckDigit() {
        Nit nit = Nit.of("900123456-8");

        assertThat(nit.value()).isEqualTo("900123456-8");
    }

    @Test
    void of_shouldThrowInvalidNitException_whenNull() {
        assertThatThrownBy(() -> Nit.of(null)).isInstanceOf(InvalidNitException.class);
    }

    @Test
    void of_shouldThrowInvalidNitException_whenBlank() {
        assertThatThrownBy(() -> Nit.of("   ")).isInstanceOf(InvalidNitException.class);
    }

    @Test
    void of_shouldThrowInvalidNitException_whenFormatIsWrong() {
        assertThatThrownBy(() -> Nit.of("not-a-nit")).isInstanceOf(InvalidNitException.class);
    }

    @Test
    void of_shouldThrowInvalidNitException_whenMissingCheckDigitSeparator() {
        assertThatThrownBy(() -> Nit.of("1234567890")).isInstanceOf(InvalidNitException.class);
    }

    @Test
    void of_shouldThrowInvalidNitException_whenCheckDigitIsWrong() {
        assertThatThrownBy(() -> Nit.of("123456789-0")).isInstanceOf(InvalidNitException.class);
    }

    @Test
    void fromPersisted_shouldNotValidate_evenWithGarbageValue() {
        Nit nit = Nit.fromPersisted("whatever-was-already-stored");

        assertThat(nit.value()).isEqualTo("whatever-was-already-stored");
    }

    @Test
    void equals_shouldBeBasedOnValue() {
        assertThat(Nit.of("123456789-6")).isEqualTo(Nit.of("123456789-6"));
        assertThat(Nit.of("123456789-6")).isNotEqualTo(Nit.of("900123456-8"));
    }
}
