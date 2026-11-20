package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.InvalidScheduleRangeException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleRangesTest {

    private static final OffsetDateTime FROM = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void requireValid_shouldRejectMissingBounds() {
        assertThatThrownBy(() -> ScheduleRanges.requireValid(null, FROM.plusDays(1)))
                .isInstanceOf(InvalidScheduleRangeException.class);
        assertThatThrownBy(() -> ScheduleRanges.requireValid(FROM, null))
                .isInstanceOf(InvalidScheduleRangeException.class);
    }

    @Test
    void requireValid_shouldRejectToBeforeOrEqualFrom() {
        assertThatThrownBy(() -> ScheduleRanges.requireValid(FROM, FROM))
                .isInstanceOf(InvalidScheduleRangeException.class);
        assertThatThrownBy(() -> ScheduleRanges.requireValid(FROM, FROM.minusDays(1)))
                .isInstanceOf(InvalidScheduleRangeException.class);
    }

    @Test
    void requireValid_shouldRejectRangesLongerThan92Days() {
        assertThatThrownBy(() -> ScheduleRanges.requireValid(FROM, FROM.plusDays(93)))
                .isInstanceOf(InvalidScheduleRangeException.class)
                .hasMessageContaining("92");
    }

    @Test
    void requireValid_shouldAcceptExactly92Days() {
        assertThatCode(() -> ScheduleRanges.requireValid(FROM, FROM.plusDays(92))).doesNotThrowAnyException();
    }
}
