package com.MyAnimaLog.Veterinary.application.patients.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListClientsResponse {

    private List<Client> items;
    private int page;
    private int size;
    private long total;

    /**
     * {@code name}/{@code email} prefieren el dato vivo de {@code UserServicePort} y caen al
     * snapshot del link si User no responde (nunca al revés): el snapshot es solo para buscar.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Client {
        private UUID userId;
        private String name;
        private String email;
        private LocalDateTime linkedAt;
        private long sharedPetsCount;
    }
}
