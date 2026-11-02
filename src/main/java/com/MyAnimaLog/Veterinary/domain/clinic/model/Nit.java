package com.MyAnimaLog.Veterinary.domain.clinic.model;

import com.MyAnimaLog.Veterinary.domain.clinic.exceptions.InvalidNitException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Colombian NIT (Número de Identificación Tributaria), format {@code 123456789-0}.
 * {@link #of(String)} validates the format and the DIAN check digit; {@link #fromPersisted(String)}
 * trusts an already-stored value without re-validating it (used when reading from the database,
 * so a row that predates this validation never fails to load).
 */
public final class Nit {

    private static final Pattern FORMAT = Pattern.compile("^(\\d{9})-(\\d)$");

    // DIAN weights for the check-digit algorithm, aligned left-to-right with the 9 base digits.
    private static final int[] WEIGHTS = {41, 37, 29, 23, 19, 17, 13, 7, 3};

    private final String value;

    private Nit(String value) {
        this.value = value;
    }

    public static Nit of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidNitException();
        }
        Matcher matcher = FORMAT.matcher(raw.trim());
        if (!matcher.matches()) {
            throw new InvalidNitException();
        }
        String base = matcher.group(1);
        int providedCheckDigit = Integer.parseInt(matcher.group(2));
        if (computeCheckDigit(base) != providedCheckDigit) {
            throw new InvalidNitException("NIT check digit does not match " + raw);
        }
        return new Nit(raw.trim());
    }

    public static Nit fromPersisted(String value) {
        return new Nit(value);
    }

    private static int computeCheckDigit(String base) {
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (base.charAt(i) - '0') * WEIGHTS[i];
        }
        int remainder = sum % 11;
        return remainder <= 1 ? remainder : 11 - remainder;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nit other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
