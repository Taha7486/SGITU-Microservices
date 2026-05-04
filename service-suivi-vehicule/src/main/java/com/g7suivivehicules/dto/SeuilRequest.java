package com.g7suivivehicules.dto;


import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeuilRequest {

    @NotNull(message = "vitesse max obligatoire")
    private Double vitesseMax;

    @NotNull(message = "temperature max obligatoire")
    private Double temperatureMax;

    @NotNull(message = "carburant min obligatoire")
    private Double carburantMin;
}