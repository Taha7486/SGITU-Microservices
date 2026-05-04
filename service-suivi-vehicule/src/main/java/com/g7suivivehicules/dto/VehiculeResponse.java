package com.g7suivivehicules.dto;



import com.g7suivivehicules.entity.Vehicule;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculeResponse {

    private UUID id;
    private String immatriculation;
    private Vehicule.TypeVehicule type;
    private String ligne;
    private Vehicule.StatutVehicule statut;
    private UUID conducteurId;
}