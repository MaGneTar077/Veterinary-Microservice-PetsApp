package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solo el código: quién se vincula sale del token, nunca del cuerpo (mismo criterio que todo el
 * resto del servicio desde VET-05). El código va en el cuerpo y no en la ruta para que no quede en
 * los logs de acceso.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkToVeterinaryRequest {
    private String code;
}
