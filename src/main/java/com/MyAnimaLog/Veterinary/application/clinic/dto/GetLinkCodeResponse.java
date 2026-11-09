package com.MyAnimaLog.Veterinary.application.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * {@code inviteCode}/{@code inviteLink} vienen en {@code null} si la clínica nunca generó uno (o si
 * lo borró): es un estado normal que el frontend pinta como "todavía no tenés código", no un error.
 * {@code hasCode} está para que no haya que inferirlo de un {@code null}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetLinkCodeResponse {
    private UUID veterinaryId;
    private String inviteCode;
    private String inviteLink;
    private boolean hasCode;
}
