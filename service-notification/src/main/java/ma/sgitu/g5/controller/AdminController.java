package ma.sgitu.g5.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.entity.NotificationStatus;
import ma.sgitu.g5.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AdminController - Endpoints d'administration pour le service de notification
 * Nécessite le rôle ROLE_ADMIN
 * Toutes les opérations sont loggées dans le fichier admin-operations.log
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "API d'administration - Nécessite ROLE_ADMIN")
public class AdminController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/stats")
    @Operation(
            summary = "Statistiques globales",
            description = "Retourne les statistiques de notifications par statut et par canal"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistiques retournées avec succès"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - ROLE_ADMIN requis")
    })
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("[ADMIN] Récupération des statistiques globales");
        
        Map<String, Object> stats = new LinkedHashMap<>();
        
        // Statistiques par statut
        Map<String, Long> statusStats = new HashMap<>();
        statusStats.put("PENDING", notificationRepository.countByStatus(NotificationStatus.PENDING));
        statusStats.put("SENT", notificationRepository.countByStatus(NotificationStatus.SENT));
        statusStats.put("FAILED", notificationRepository.countByStatus(NotificationStatus.FAILED));
        stats.put("byStatus", statusStats);
        
        // Statistiques par canal
        Map<String, Long> channelStats = new HashMap<>();
        channelStats.put("EMAIL", notificationRepository.countByChannel("EMAIL"));
        channelStats.put("SMS", notificationRepository.countByChannel("SMS"));
        channelStats.put("PUSH", notificationRepository.countByChannel("PUSH"));
        stats.put("byChannel", channelStats);
        
        // Total
        stats.put("total", notificationRepository.count());
        stats.put("timestamp", LocalDateTime.now().toString());
        
        log.info("[ADMIN] Statistiques récupérées: {}", stats);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/notifications")
    @Operation(
            summary = "Lister toutes les notifications (admin)",
            description = "Retourne la liste paginée de toutes les notifications avec filtres avancés"
    )
    @ApiResponse(responseCode = "200", description = "Liste retournée avec succès")
    public ResponseEntity<Page<Notification>> listAllNotifications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String sourceService,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        
        log.info("[ADMIN] Liste des notifications - filters: status={}, channel={}, source={}, startDate={}, endDate={}", 
                status, channel, sourceService, startDate, endDate);
        
        Page<Notification> notifications;
        
        if (status != null && channel != null) {
            notifications = notificationRepository.findByStatusAndChannel(
                    NotificationStatus.valueOf(status), channel, pageable);
        } else if (status != null) {
            notifications = notificationRepository.findByStatus(
                    NotificationStatus.valueOf(status), pageable);
        } else if (channel != null) {
            notifications = notificationRepository.findByChannel(channel, pageable);
        } else if (sourceService != null) {
            notifications = notificationRepository.findBySourceService(sourceService, pageable);
        } else {
            notifications = notificationRepository.findAll(pageable);
        }
        
        log.info("[ADMIN] {} notifications trouvées", notifications.getTotalElements());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/notifications/failed")
    @Operation(
            summary = "Lister les notifications en échec",
            description = "Retourne uniquement les notifications avec le statut FAILED"
    )
    @ApiResponse(responseCode = "200", description = "Liste des échecs retournée avec succès")
    public ResponseEntity<Page<Notification>> listFailedNotifications(Pageable pageable) {
        log.info("[ADMIN] Récupération des notifications en échec");
        
        Page<Notification> failed = notificationRepository.findByStatus(
                NotificationStatus.FAILED, pageable);
        
        log.info("[ADMIN] {} notifications en échec trouvées", failed.getTotalElements());
        return ResponseEntity.ok(failed);
    }

    @GetMapping("/notifications/by-source/{sourceService}")
    @Operation(
            summary = "Lister les notifications par service source",
            description = "Retourne les notifications provenant d'un service spécifique (G1-G10)"
    )
    @ApiResponse(responseCode = "200", description = "Liste retournée avec succès")
    public ResponseEntity<Page<Notification>> listBySourceService(
            @PathVariable String sourceService,
            Pageable pageable) {
        
        log.info("[ADMIN] Récupération des notifications pour le service source: {}", sourceService);
        
        Page<Notification> notifications = notificationRepository.findBySourceService(
                sourceService.toUpperCase(), pageable);
        
        log.info("[ADMIN] {} notifications trouvées pour {}", notifications.getTotalElements(), sourceService);
        return ResponseEntity.ok(notifications);
    }

    @DeleteMapping("/notifications/{id}")
    @Operation(
            summary = "Supprimer une notification (admin)",
            description = "Supprime définitivement une notification par son ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification supprimée avec succès"),
            @ApiResponse(responseCode = "404", description = "Notification introuvable")
    })
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        log.warn("[ADMIN] Suppression de la notification ID: {}", id);
        
        if (!notificationRepository.existsById(id)) {
            log.error("[ADMIN] Notification ID {} introuvable", id);
            return ResponseEntity.notFound().build();
        }
        
        notificationRepository.deleteById(id);
        log.info("[ADMIN] Notification ID {} supprimée avec succès", id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification supprimée avec succès");
        response.put("id", id.toString());
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/{id}/force-retry")
    @Operation(
            summary = "Forcer la relance d'une notification (admin)",
            description = "Force la relance d'une notification quel que soit son statut"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relance forcée avec succès"),
            @ApiResponse(responseCode = "404", description = "Notification introuvable")
    })
    public ResponseEntity<Map<String, String>> forceRetry(@PathVariable Long id) {
        log.warn("[ADMIN] Force retry pour la notification ID: {}", id);
        
        Notification notification = notificationRepository.findById(id)
                .orElse(null);
        
        if (notification == null) {
            log.error("[ADMIN] Notification ID {} introuvable", id);
            return ResponseEntity.notFound().build();
        }
        
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);
        notificationRepository.save(notification);
        
        log.info("[ADMIN] Force retry effectué pour notification ID: {}", id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Relance forcée avec succès");
        response.put("notificationId", notification.getNotificationId());
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/detailed")
    @Operation(
            summary = "Health check détaillé (admin)",
            description = "Retourne des informations détaillées sur l'état du service"
    )
    @ApiResponse(responseCode = "200", description = "État détaillé retourné avec succès")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        log.info("[ADMIN] Health check détaillé demandé");
        
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "notification-service");
        health.put("version", "1.0.0");
        
        // Statistiques de base
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalNotifications", notificationRepository.count());
        metrics.put("pendingNotifications", notificationRepository.countByStatus(NotificationStatus.PENDING));
        metrics.put("failedNotifications", notificationRepository.countByStatus(NotificationStatus.FAILED));
        health.put("metrics", metrics);
        
        log.info("[ADMIN] Health check détaillé: {}", health);
        return ResponseEntity.ok(health);
    }
}
