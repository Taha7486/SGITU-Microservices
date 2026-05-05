package ma.sgitu.g5.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.request.MetadataDTO;
import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.request.RecipientDTO;
import ma.sgitu.g5.service.INotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * KafkaConsumerController — Consommateur centralisé pour SGITU.
 * Utilise des patterns pour capturer dynamiquement tous les topics de G1, G2 et G3.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerController {

    private final INotificationService notificationService;
    private final ObjectMapper objectMapper;

    // ── G1 — Billetterie : Consomme TOUS les topics ticket.* (11 topics) ──
    @KafkaListener(
        topicPattern = "ticket\\..*", 
        groupId = "notification-group", 
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTicketEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.info("[KAFKA-G1] Event reçu sur topic={} | partition={} | offset={}", 
                record.topic(), record.partition(), record.offset());
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String eventType = (String) event.get("eventType");
            String userId = String.valueOf(event.get("userId"));
            String ticketId = String.valueOf(event.getOrDefault("ticketId", "unknown"));

            // Flux G1 exige EMAIL + PUSH pour les notifications de ticket
            List<String> channels = List.of("EMAIL", "PUSH");
            
            Map<String, Object> metadataMap = new HashMap<>(event);
            metadataMap.remove("eventType");
            metadataMap.remove("userId");

            for (String channel : channels) {
                // ID déterministe pour garantir l'idempotence (évite les doublons)
                String deterministicId = "G1-" + ticketId + "-" + eventType + "-" + channel;
                
                NotificationRequestDTO dto = buildDto(
                    deterministicId, "G1_BILLETTERIE", eventType, channel, "NORMAL", userId, event, metadataMap
                );
                notificationService.send(dto);
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA-G1] Échec du traitement : {}", e.getMessage());
        }
    }

    // ── G2 — Abonnements : Consomme TOUS les topics abonnement.* (7 topics) ──
    @KafkaListener(
        topicPattern = "abonnement\\..*", 
        groupId = "notification-group", 
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAbonnementEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.info("[KAFKA-G2] Event reçu sur topic={} | offset={}", record.topic(), record.offset());
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            
            // G2 utilise le champ "type" au lieu de "eventType"
            String eventType = (String) event.getOrDefault("type", "UNKNOWN_ABONNEMENT");
            String userId = String.valueOf(event.get("userId"));
            String abonnementId = String.valueOf(event.getOrDefault("abonnementId", "0"));

            // G2 fournit une liste de canaux (ex: ["EMAIL", "SMS"])
            List<String> channels = (List<String>) event.getOrDefault("channels", List.of("EMAIL"));
            Map<String, Object> data = (Map<String, Object>) event.getOrDefault("data", new HashMap<>());

            for (String channel : channels) {
                String deterministicId = "G2-" + abonnementId + "-" + eventType + "-" + channel;
                
                NotificationRequestDTO dto = buildDto(
                    deterministicId, "G2_ABONNEMENT", eventType, channel, "NORMAL", userId, event, data
                );
                notificationService.send(dto);
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA-G2] Échec du traitement : {}", e.getMessage());
        }
    }

    // ── G3 — Utilisateurs : Topic unique user-events ───────────────────────
    @KafkaListener(
        topics = "user-events", 
        groupId = "notification-group", 
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.info("[KAFKA-G3] Event reçu sur user-events | offset={}", record.offset());
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String eventType = (String) event.get("eventType");
            String userId = (String) event.get("userId");

            // G3 utilise principalement l'EMAIL
            String deterministicId = "G3-" + userId + "-" + eventType;

            NotificationRequestDTO dto = buildDto(
                deterministicId, "G3_UTILISATEUR", eventType, "EMAIL", "HIGH", userId, event, event
            );
            notificationService.send(dto);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA-G3] Échec du traitement : {}", e.getMessage());
        }
    }

    private NotificationRequestDTO buildDto(String id, String source, String type, String channel, 
                                          String priority, String userId, Map<String, Object> rawEvent, 
                                          Map<String, Object> metadata) {
        NotificationRequestDTO dto = new NotificationRequestDTO();
        dto.setNotificationId(id);
        dto.setSourceService(source);
        dto.setEventType(type);
        dto.setChannel(channel);
        dto.setPriority(priority);
        
        RecipientDTO recipient = new RecipientDTO();
        recipient.setUserId(userId);
        // TODO: Appeler le service G3 (Utilisateurs) pour résoudre l'email/téléphone à partir de l'userId
        recipient.setEmail((String) rawEvent.getOrDefault("email", ""));
        recipient.setPhone((String) rawEvent.getOrDefault("phone", ""));
        recipient.setDeviceToken((String) rawEvent.getOrDefault("deviceToken", ""));
        dto.setRecipient(recipient);

        MetadataDTO m = new MetadataDTO();
        m.setData(metadata);
        dto.setMetadata(m);
        return dto;
    }
}
