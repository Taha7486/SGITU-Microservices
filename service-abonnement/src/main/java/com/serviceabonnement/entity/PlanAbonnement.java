package com.serviceabonnement.entity;

import com.serviceabonnement.enums.CategorieAbonnement;
import com.serviceabonnement.enums.DureeOffre;
import com.serviceabonnement.enums.MoyenTransport;
import com.serviceabonnement.enums.StatutOffre;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "plans_abonnement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanAbonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String description;

    @Column(nullable = false)
    private BigDecimal prix;

    @Enumerated(EnumType.STRING)
    private DureeOffre duree;

    @Enumerated(EnumType.STRING)
    private CategorieAbonnement categorie;

    @Enumerated(EnumType.STRING)
    private MoyenTransport moyenTransport;

    @Enumerated(EnumType.STRING)
    private StatutOffre statut;
}
