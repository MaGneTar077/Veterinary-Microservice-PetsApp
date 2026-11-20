package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryRequest;
import com.MyAnimaLog.Veterinary.domain.schedule.exceptions.InvalidBusinessHoursException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleEntriesTest {

    private static final java.util.function.Function<String, RuntimeException> ON_ERROR =
            InvalidBusinessHoursException::new;

    private ScheduleEntryRequest entry(Integer day, String opens, String closes) {
        return ScheduleEntryRequest.builder().dayOfWeek(day).opensAt(opens).closesAt(closes).build();
    }

    private List<ScheduleEntries.Entry> validate(List<ScheduleEntryRequest> entries) {
        return ScheduleEntries.validate(entries, ON_ERROR);
    }

    @Test
    void validate_shouldAcceptAnEmptyWeek() {
        assertThat(validate(List.of())).isEmpty();
        assertThat(validate(null)).isEmpty();
    }

    @Test
    void validate_shouldSortByDayAndOpeningTime() {
        List<ScheduleEntries.Entry> result = validate(List.of(
                entry(3, "14:00", "18:00"),
                entry(1, "08:00", "12:00"),
                entry(3, "08:00", "12:00")));

        assertThat(result).extracting(ScheduleEntries.Entry::dayOfWeek).containsExactly(1, 3, 3);
        assertThat(ScheduleEntries.format(result.get(1).opensAt())).isEqualTo("08:00");
    }

    @Test
    void validate_shouldRejectOverlappingEntriesOnTheSameDay() {
        assertThatThrownBy(() -> validate(List.of(
                entry(1, "08:00", "13:00"),
                entry(1, "12:00", "17:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class)
                .hasMessageContaining("Overlapping");
    }

    @Test
    void validate_shouldAcceptAdjacentEntriesOnTheSameDay() {
        assertThat(validate(List.of(
                entry(1, "08:00", "12:00"),
                entry(1, "12:00", "17:00")))).hasSize(2);
    }

    @Test
    void validate_shouldAllowTheSameTimesOnDifferentDays() {
        assertThat(validate(List.of(
                entry(1, "08:00", "12:00"),
                entry(2, "08:00", "12:00")))).hasSize(2);
    }

    @Test
    void validate_shouldRejectClosingBeforeOrAtOpening() {
        assertThatThrownBy(() -> validate(List.of(entry(1, "12:00", "12:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);
        assertThatThrownBy(() -> validate(List.of(entry(1, "18:00", "09:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);
    }

    @Test
    void validate_shouldRejectDaysOutsideIsoRange() {
        assertThatThrownBy(() -> validate(List.of(entry(0, "08:00", "12:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);
        assertThatThrownBy(() -> validate(List.of(entry(8, "08:00", "12:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);
        assertThatThrownBy(() -> validate(List.of(entry(null, "08:00", "12:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);
    }

    @Test
    void validate_shouldRejectTimesThatAreNotStrictHhMm() {
        assertThatThrownBy(() -> validate(List.of(entry(1, "8:00", "12:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);
        assertThatThrownBy(() -> validate(List.of(entry(1, "08:00:00", "12:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);
        assertThatThrownBy(() -> validate(List.of(entry(1, "25:00", "26:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);
        assertThatThrownBy(() -> validate(List.of(entry(1, null, "12:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class);
    }

    @Test
    void validate_shouldRejectMoreThanFourEntriesPerDay() {
        assertThatThrownBy(() -> validate(List.of(
                entry(1, "07:00", "08:00"),
                entry(1, "09:00", "10:00"),
                entry(1, "11:00", "12:00"),
                entry(1, "13:00", "14:00"),
                entry(1, "15:00", "16:00"))))
                .isInstanceOf(InvalidBusinessHoursException.class)
                .hasMessageContaining("At most 4");
    }

    @Test
    void validate_shouldAcceptFourEntriesPerDay() {
        assertThat(validate(List.of(
                entry(1, "07:00", "08:00"),
                entry(1, "09:00", "10:00"),
                entry(1, "11:00", "12:00"),
                entry(1, "13:00", "14:00")))).hasSize(4);
    }
}
