package com.g7suivivehicules.service;

import com.g7suivivehicules.dto.G5NotificationRequest;
import com.g7suivivehicules.entity.Alert;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class G5NotificationService {

    private final RestTemplate restTemplate;

    @Value("${g5.notification.url}")
    private String g5Url;

    @CircuitBreaker(name = "g5NotificationService", fallbackMethod = "notifierConducteurFallback")
    @Retry(name = "g5NotificationService")
    @TimeLimiter(name = "g5NotificationService")
    public CompletableFuture<Void> notifierConducteur(Alert alert) {
        try {
            String priority = determinerPriorite(alert.getTypeAlert());
            String message = genererMessageConducteur(alert);

            // Récupération de l'email depuis le contexte de sécurité (JWT via Gateway)
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication() != null 
                    ? SecurityContextHolder.getContext().getAuthentication().getName() 
                    : "conducteur@sgitu.ma";

            Map<String, String> metadata = new HashMap<>();
            metadata.put("vehiculeId", alert.getVehiculeId().toString());
            metadata.put("typeAnomalie", alert.getTypeAlert().name());
            metadata.put("valeur", alert.getValeur() != null ? alert.getValeur().toString() : "N/A");
            metadata.put("seuil", alert.getSeuil() != null ? alert.getSeuil().toString() : "N/A");
            metadata.put("message", message);
            metadata.put("timestamp", alert.getTimestampDebut().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            G5NotificationRequest request = G5NotificationRequest.builder()
                    .notificationId("uuid-g7-" + UUID.randomUUID().toString().substring(0, 8))
                    .eventType("VEHICULE_ALERTE_CONDUCTEUR")
                    .priority(priority)
                    .recipient(G5NotificationRequest.Recipient.builder()
                            .userId("conducteur-" + alert.getVehiculeId())
                            .email(currentUserEmail)
                            .deviceToken("token-device-conducteur-" + alert.getVehiculeId())
                            .build())
                    .metadata(metadata)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<G5NotificationRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(g5Url, entity, String.class);

            log.info("[G5Notification] Notification PUSH envoyée pour véhicule {} (Alerte: {})",
                    alert.getVehiculeId(), alert.getTypeAlert());

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("[G5Notification] Échec de l'envoi vers G5 : {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Void> notifierConducteurFallback(Alert alert, Exception e) {
        log.warn("[G5Notification] Circuit breaker activé - Notification fallback pour véhicule {}", alert.getVehiculeId());
        return CompletableFuture.completedFuture(null);
    }

    private String determinerPriorite(Alert.TypeAlert type) {
        switch (type) {
            case TEMPERATURE_CRITIQUE:
            case CARBURANT_CRITIQUE:
            case VITESSE_EXCESSIVE:
                return "HIGH";
            default:
                return "NORMAL";
        }
    }

    private String genererMessageConducteur(Alert alert) {
        switch (alert.getTypeAlert()) {
            case TEMPERATURE_CRITIQUE:
                return "Température moteur critique, arrêtez le véhicule";
            case CARBURANT_CRITIQUE:
                return "Niveau carburant très bas, faites le plein";
            case VITESSE_EXCESSIVE:
                return "Vitesse excessive, réduisez la vitesse";
            case FREINAGE_BRUSQUE:
                return "Freinage brusque détecté, soyez prudent";
            case IMMOBILISATION:
                return "Arrêt anormal détecté, vérifiez la situation";
            case DEVIATION_ITINERAIRE:
                return "Déviation d'itinéraire détectée";
            default:
                return "Alerte de sécurité détectée sur votre véhicule";
        }
    }

    @CircuitBreaker(name = "g5NotificationService", fallbackMethod = "notifierLogAdminFallback")
    @Retry(name = "g5NotificationService")
    @TimeLimiter(name = "g5NotificationService")
    public CompletableFuture<Void> notifierLogAdmin(String logLevel, String message) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("logLevel", logLevel);
            metadata.put("serviceName", "G7_SUIVI_VEHICULES");
            metadata.put("message", message);
            metadata.put("timestamp", java.time.LocalDateTime.now().toString());
            metadata.put("source", "G7");

            G5NotificationRequest request = G5NotificationRequest.builder()
                    .notificationId("uuid-g7-" + UUID.randomUUID().toString().substring(0, 8))
                    .eventType("LOG_ALERT_ADMIN")
                    .channel("EMAIL")
                    .priority(determinePriorityFromLogLevel(logLevel))
                    .recipient(G5NotificationRequest.Recipient.builder()
                            .userId("admin")
                            .email("admin@sgitu.ma")
                            .deviceToken(null)
                            .build())
                    .metadata(metadata)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<G5NotificationRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(g5Url, entity, String.class);

            log.info("[G5Notification] Notification LOG envoyée à l'admin - Level: {}, Message: {}", logLevel, message);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("[G5Notification] Échec de l'envoi de log vers G5 : {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    @CircuitBreaker(name = "g5NotificationService", fallbackMethod = "notifierVehiculeEnregistreFallback")
    @Retry(name = "g5NotificationService")
    @TimeLimiter(name = "g5NotificationService")
    public CompletableFuture<Void> notifierVehiculeEnregistre(com.g7suivivehicules.dto.VehiculeResponse vehicule) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("vehiculeId", vehicule.getId().toString());
            metadata.put("immatriculation", vehicule.getImmatriculation());
            metadata.put("type", vehicule.getType().name());
            metadata.put("message", "Votre véhicule a été enregistré avec succès dans le système SGITU.");
            metadata.put("timestamp", java.time.LocalDateTime.now().toString());

            G5NotificationRequest request = G5NotificationRequest.builder()
                    .notificationId("uuid-g7-" + UUID.randomUUID().toString().substring(0, 8))
                    .eventType("VEHICULE_ENREGISTRE")
                    .priority("NORMAL")
                    .recipient(G5NotificationRequest.Recipient.builder()
                            .userId("conducteur-" + vehicule.getConducteurId())
                            .email("conducteur@sgitu.ma") // Idéalement récupéré depuis un service User
                            .deviceToken(null)
                            .build())
                    .metadata(metadata)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<G5NotificationRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(g5Url, entity, String.class);

            log.info("[G5Notification] Notification d'enregistrement envoyée pour : {}", vehicule.getImmatriculation());

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("[G5Notification] Échec de l'envoi de notification d'enregistrement : {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Void> notifierVehiculeEnregistreFallback(com.g7suivivehicules.dto.VehiculeResponse vehicule, Exception e) {
        log.warn("[G5Notification] Circuit breaker activé - Fallback enregistrement pour : {}", vehicule.getImmatriculation());
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> notifierLogAdminFallback(String logLevel, String message, Exception e) {
        log.warn("[G5Notification] Circuit breaker activé - Notification log fallback - Level: {}", logLevel);
        // Optionnel: loguer le message localement
        return CompletableFuture.completedFuture(null);
    }

    private String determinePriorityFromLogLevel(String logLevel) {
        return switch (logLevel.toUpperCase()) {
            case "ERROR", "FATAL" -> "HIGH";
            case "WARN" -> "NORMAL";
            default -> "LOW";
        };
    }
}
