package com.serviceabonnement.entity;

import com.serviceabonnement.enums.StatutRenouvellement;
import com.serviceabonnement.enums.TypeRenouvellement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "renouvellements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Renouvellement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "abonnement_id", nullable = false)
    private Abonnement abonnement;

    @Column(nullable = false)
    private LocalDateTime dateRenouvellement;

    @Column(nullable = false)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    private StatutRenouvellement statut;

    @Enumerated(EnumType.STRING)
    private TypeRenouvellement type;
}
