package com.MyAnimaLog.Veterinary.application.schedule.ports.out;

import com.MyAnimaLog.Veterinary.domain.schedule.model.BusinessHours;

import java.util.List;
import java.util.UUID;

public interface BusinessHoursRepositoryPort {

    List<BusinessHours> findByVeterinaryId(UUID veterinaryId);

    /**
     * Borra la semana completa. Se llama siempre dentro de la misma transacción que el
     * {@code saveAll} que la repuebla ({@code ReplaceBusinessHoursService}), para que un fallo al
     * insertar deje la semana anterior intacta.
     */
    void deleteByVeterinaryId(UUID veterinaryId);

    List<BusinessHours> saveAll(List<BusinessHours> hours);
}
