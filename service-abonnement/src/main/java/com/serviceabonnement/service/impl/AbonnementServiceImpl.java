package com.serviceabonnement.service.impl;

import com.serviceabonnement.dto.request.SouscriptionRequestDTO;
import com.serviceabonnement.dto.response.AbonnementResponseDTO;
import com.serviceabonnement.repository.AbonnementRepository;
import com.serviceabonnement.service.AbonnementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AbonnementServiceImpl implements AbonnementService {

    private final AbonnementRepository abonnementRepository;

    @Override
    public AbonnementResponseDTO souscrire(SouscriptionRequestDTO request) {
        // Logique de souscription à implémenter
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public AbonnementResponseDTO getAbonnementById(Long id) {
        // Logique de récupération à implémenter
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbonnementResponseDTO> getAbonnementsByUtilisateur(Long utilisateurId) {
        // Logique de récupération à implémenter
        return List.of();
    }

    @Override
    public void resilier(Long id) {
        // Logique de résiliation à implémenter
    }
}
