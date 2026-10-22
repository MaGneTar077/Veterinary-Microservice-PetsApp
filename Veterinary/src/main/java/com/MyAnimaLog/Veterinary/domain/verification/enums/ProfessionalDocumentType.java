package com.MyAnimaLog.Veterinary.domain.verification.enums;

public enum ProfessionalDocumentType {
    PROFESSIONAL_CARD(true),
    ID_DOCUMENT(true),
    DEGREE(false);

    private final boolean required;

    ProfessionalDocumentType(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }
}
