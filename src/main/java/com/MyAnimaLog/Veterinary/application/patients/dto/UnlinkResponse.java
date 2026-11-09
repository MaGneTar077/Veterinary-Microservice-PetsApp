package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * {@code revokedPetsCount} es cuántas mascotas dejó de ver la clínica por esta desvinculación. Va en
 * la respuesta para que el frontend pueda confirmárselo al dueño ("3 mascotas dejaron de ser
 * visibles"): la cascada es la consecuencia menos obvia de desvincularse.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnlinkResponse {
    private UUID userId;
    private UUID veterinaryId;
    private int revokedPetsCount;
    private String message;
}
