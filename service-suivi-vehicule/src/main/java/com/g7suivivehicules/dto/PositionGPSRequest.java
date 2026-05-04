package com.g7suivivehicules.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionGPSRequest {

    @NotNull(message = "vehiculeId obligatoire")
    private UUID vehiculeId;

    @NotNull(message = "latitude obligatoire")
    private Double latitude;

    @NotNull(message = "longitude obligatoire")
    private Double longitude;

    private Double vitesse;
    private Double cap;
}