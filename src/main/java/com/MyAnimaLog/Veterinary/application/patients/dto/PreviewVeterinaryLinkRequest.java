package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * El código viaja en el <strong>cuerpo</strong>, no en la ruta: el gateway y Cloud Run loguean la
 * URL completa, y un código de vinculación en un log es un código filtrado (mismo criterio que el
 * token de invitación de la Fase 3).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreviewVeterinaryLinkRequest {
    private String code;
}
