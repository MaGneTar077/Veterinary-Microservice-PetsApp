package com.MyAnimaLog.Veterinary.application.schedule.services;

import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryRequest;
import com.MyAnimaLog.Veterinary.application.schedule.dto.ScheduleEntryResponse;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Validación y formato de las franjas horarias, compartidos por el horario de la clínica y el de un
 * empleado: las dos tienen exactamente las mismas reglas y escribirlas dos veces garantizaría que
 * se separen con el tiempo. Utilidad estática, no un puerto ni un servicio.
 *
 * <p>Las horas viajan como {@code "HH:mm"} (2 dígitos y 2 dígitos). Se rechaza cualquier otra forma
 * — {@code "8:00"}, {@code "08:00:00"} o {@code "25:00"} — en vez de normalizarla en silencio.
 */
public final class ScheduleEntries {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_ENTRIES_PER_DAY = 4;

    private ScheduleEntries() {
    }

    /** Una franja ya validada y parseada, lista para construir el modelo de dominio. */
    public record Entry(int dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
    }

    public static String format(LocalTime time) {
        return time.format(HH_MM);
    }

    /**
     * Valida la lista completa (una lista vacía es válida: la clínica o el empleado sin horario) y
     * la devuelve parseada y ordenada por día y hora de apertura.
     *
     * @param onError fábrica de la excepción de dominio que corresponda al recurso que se está
     *                validando ({@code InvalidBusinessHoursException} o
     *                {@code InvalidEmployeeScheduleException}), para que el {@code code} del error
     *                HTTP sea el del recurso y no uno genérico
     */
    public static List<Entry> validate(List<ScheduleEntryRequest> requested,
                                        Function<String, RuntimeException> onError) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }

        List<Entry> entries = new ArrayList<>();
        for (ScheduleEntryRequest entry : requested) {
            entries.add(parse(entry, onError));
        }

        entries.sort(Comparator.comparingInt(Entry::dayOfWeek).thenComparing(Entry::opensAt));
        requireNoOverlapAndWithinDailyLimit(entries, onError);
        return List.copyOf(entries);
    }

    public static List<ScheduleEntryResponse> toResponse(List<Entry> entries) {
        return entries.stream()
                .map(entry -> ScheduleEntryResponse.builder()
                        .dayOfWeek(entry.dayOfWeek())
                        .opensAt(format(entry.opensAt()))
                        .closesAt(format(entry.closesAt()))
                        .build())
                .toList();
    }

    private static Entry parse(ScheduleEntryRequest entry, Function<String, RuntimeException> onError) {
        if (entry == null) {
            throw onError.apply("Schedule entries cannot be null");
        }
        if (entry.getDayOfWeek() == null || entry.getDayOfWeek() < 1 || entry.getDayOfWeek() > 7) {
            throw onError.apply("dayOfWeek must be between 1 (Monday) and 7 (Sunday)");
        }
        LocalTime opensAt = parseTime(entry.getOpensAt(), "opensAt", onError);
        LocalTime closesAt = parseTime(entry.getClosesAt(), "closesAt", onError);
        if (!closesAt.isAfter(opensAt)) {
            throw onError.apply("closesAt must be after opensAt on day " + entry.getDayOfWeek());
        }
        return new Entry(entry.getDayOfWeek(), opensAt, closesAt);
    }

    private static LocalTime parseTime(String raw, String field, Function<String, RuntimeException> onError) {
        if (raw == null || raw.isBlank()) {
            throw onError.apply(field + " is required");
        }
        try {
            return LocalTime.parse(raw.trim(), HH_MM);
        } catch (DateTimeParseException ex) {
            throw onError.apply(field + " must be a valid time in HH:mm format, got: " + raw);
        }
    }

    /**
     * Dos franjas del mismo día se solapan si una empieza antes de que termine la otra. Pegadas
     * ({@code 08:00-12:00} y {@code 12:00-17:00}) <strong>no</strong> se solapan: son válidas.
     */
    private static void requireNoOverlapAndWithinDailyLimit(List<Entry> sorted,
                                                             Function<String, RuntimeException> onError) {
        for (int day = 1; day <= 7; day++) {
            int finalDay = day;
            List<Entry> ofDay = sorted.stream().filter(entry -> entry.dayOfWeek() == finalDay).toList();
            if (ofDay.size() > MAX_ENTRIES_PER_DAY) {
                throw onError.apply("At most " + MAX_ENTRIES_PER_DAY + " entries per day, day "
                        + day + " has " + ofDay.size());
            }
            for (int i = 1; i < ofDay.size(); i++) {
                if (ofDay.get(i).opensAt().isBefore(ofDay.get(i - 1).closesAt())) {
                    throw onError.apply("Overlapping entries on day " + day);
                }
            }
        }
    }
}
