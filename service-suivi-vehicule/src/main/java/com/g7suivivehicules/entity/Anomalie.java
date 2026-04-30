package com.g7suivivehicules.entity;



import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "anomalies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anomalie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID vehiculeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAnomalie type;

    private Double valeur;

    private Double seuil;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Boolean traitee = false;

    public enum TypeAnomalie {
        AUCUNE,
        ARRET_ANORMAL,
        VITESSE_EXCESSIVE,
        TEMPERATURE_CRITIQUE,
        CARBURANT_CRITIQUE,
        FREINAGE_BRUSQUE,
        PRESSION_HUILE_CRITIQUE,
        CHUTE_DE_TENSION_BATTERIE,
        FUMMEE_DETECTEE,
        PRESSION_PNEU_FAIBLE
    }
}