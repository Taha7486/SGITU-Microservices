package com.serviceabonnement.entity;

import com.serviceabonnement.enums.StatutPaiement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "abonnement_id", nullable = false)
    private Abonnement abonnement;

    @Column(nullable = false)
    private LocalDateTime datePaiement;

    @Column(nullable = false)
    private BigDecimal montant;

    private String moyenPaiement;

    @Enumerated(EnumType.STRING)
    private StatutPaiement statut;

    private String transactionId;
}
