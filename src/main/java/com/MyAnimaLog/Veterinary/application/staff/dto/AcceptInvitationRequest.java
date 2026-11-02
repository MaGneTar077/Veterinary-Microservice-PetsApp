package com.MyAnimaLog.Veterinary.application.staff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** El token viaja en el cuerpo, nunca en la URL (ver {@link GetInvitationPreviewRequest}). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcceptInvitationRequest {
    private String token;
}
