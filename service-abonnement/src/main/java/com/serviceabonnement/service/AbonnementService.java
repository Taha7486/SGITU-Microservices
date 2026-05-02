package com.serviceabonnement.service;

import com.serviceabonnement.dto.request.SouscriptionRequestDTO;
import com.serviceabonnement.dto.response.AbonnementResponseDTO;

import java.util.List;

public interface AbonnementService {
    AbonnementResponseDTO souscrire(SouscriptionRequestDTO request);
    AbonnementResponseDTO getAbonnementById(Long id);
    List<AbonnementResponseDTO> getAbonnementsByUtilisateur(Long utilisateurId);
    void resilier(Long id);
}
