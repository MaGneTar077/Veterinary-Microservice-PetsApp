package com.MyAnimaLog.Veterinary.domain.clinic.enums;

public enum VeterinaryStatus {
    PENDING_DOCUMENTS,
    UNDER_REVIEW,
    NEEDS_CORRECTION,
    ACTIVE,
    REJECTED,
    SUSPENDED;

    /**
     * public.veterinary.active is kept in sync with this status (active = status == ACTIVE)
     * until a future cleanup script drops that column — see db/scripts/README.md.
     */
    public boolean impliesActiveFlag() {
        return this == ACTIVE;
    }
}
