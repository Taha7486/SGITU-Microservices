package com.serviceabonnement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "desactivations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Desactivation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "abonnement_id", nullable = false)
    private Abonnement abonnement;

    @Column(nullable = false)
    private LocalDateTime dateDesactivation;

    private String motif;

    private String commentaire;
}
