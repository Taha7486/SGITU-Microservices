package com.g7suivivehicules.service;

import com.g7suivivehicules.dto.G8VehiculeStatusDTO;
import com.g7suivivehicules.entity.PositionGPS;
import com.g7suivivehicules.entity.Vehicule;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class G8AnalyticsMapper {

    public G8VehiculeStatusDTO toStatusDto(Vehicule vehicule, PositionGPS position, Integer delayMinutes) {
        return G8VehiculeStatusDTO.builder()
                .timestamp(java.time.OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .vehicleId(vehicule.getId().toString())
                .line(resolveLine(vehicule.getLigne()))
                .status(mapG8Status(vehicule.getStatut()))
                .speed(position != null ? position.getVitesse() : null)
                .delayMinutes(delayMinutes != null ? Math.max(delayMinutes, 0) : 0)
                .build();
    }

    private String resolveLine(String ligne) {
        return (ligne == null || ligne.isBlank()) ? "UNKNOWN" : ligne;
    }

    private String mapG8Status(Vehicule.StatutVehicule statut) {
        return statut == Vehicule.StatutVehicule.EN_SERVICE ? "in_service" : "out_of_service";
    }
}
