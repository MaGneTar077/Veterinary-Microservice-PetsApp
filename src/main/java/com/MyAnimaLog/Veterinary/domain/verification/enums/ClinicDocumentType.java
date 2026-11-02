package com.MyAnimaLog.Veterinary.domain.verification.enums;

public enum ClinicDocumentType {
    RUT(true),
    CHAMBER_OF_COMMERCE(true),
    LEGAL_REP_ID(true),
    FACADE_PHOTO(false);

    private final boolean required;

    ClinicDocumentType(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }
}
