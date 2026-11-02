package com.MyAnimaLog.Veterinary.application.staff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * El token viaja en el cuerpo, nunca en la URL: el gateway y Cloud Run registran la ruta completa
 * de cada petición en sus logs, que no controlamos.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetInvitationPreviewRequest {
    private String token;
}
