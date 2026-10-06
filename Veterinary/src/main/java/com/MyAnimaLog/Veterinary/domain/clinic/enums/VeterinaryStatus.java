package com.MyAnimaLog.Veterinary.domain.clinic.enums;

public enum VeterinaryStatus {
    PENDING_DOCUMENTS,
    UNDER_REVIEW,
    NEEDS_CORRECTION,
    ACTIVE,
    REJECTED,
    SUSPENDED;

    public static VeterinaryStatus fromActiveFlag(boolean active) {
        return active ? ACTIVE : SUSPENDED;
    }
}
