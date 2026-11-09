package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lo que el dueño ve <strong>antes</strong> de aceptar vincularse: solo lo necesario para reconocer
 * la clínica. Nada interno —sin NIT, sin razón social, sin estado, sin correo ni teléfono de la
 * clínica, sin datos de su personal— porque cualquiera con un código puede llamar a esto.
 *
 * <p>{@code alreadyLinked} evita que la pantalla ofrezca "vincularme" cuando ya lo está.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreviewVeterinaryLinkResponse {
    private UUID veterinaryId;
    private String name;
    private String logoUrl;
    private String city;
    private String department;
    private String address;
    private boolean alreadyLinked;
}
