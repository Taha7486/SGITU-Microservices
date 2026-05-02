package com.serviceabonnement.controller;

import com.serviceabonnement.dto.request.SouscriptionRequestDTO;
import com.serviceabonnement.dto.response.AbonnementResponseDTO;
import com.serviceabonnement.service.AbonnementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/abonnements")
@RequiredArgsConstructor
public class AbonnementController {

    private final AbonnementService abonnementService;

    @PostMapping("/souscrire")
    public ResponseEntity<AbonnementResponseDTO> souscrire(@Valid @RequestBody SouscriptionRequestDTO request) {
        return new ResponseEntity<>(abonnementService.souscrire(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AbonnementResponseDTO> getAbonnement(@PathVariable Long id) {
        return ResponseEntity.ok(abonnementService.getAbonnementById(id));
    }

    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<List<AbonnementResponseDTO>> getAbonnementsByUtilisateur(@PathVariable Long utilisateurId) {
        return ResponseEntity.ok(abonnementService.getAbonnementsByUtilisateur(utilisateurId));
    }

    @DeleteMapping("/{id}/resilier")
    public ResponseEntity<Void> resilier(@PathVariable Long id) {
        abonnementService.resilier(id);
        return ResponseEntity.noContent().build();
    }
}
