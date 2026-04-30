package com.g7suivivehicules.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "telemetries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Telemetrie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID vehiculeId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private Double vitesse;

    private Double temperature;

    private Double carburant;

    private EtatVehicule etat;

    public enum EtatVehicule {
        EN_MARCHE, A_LARRET, EN_PANNE
    }
}
