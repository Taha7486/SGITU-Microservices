package ma.sgitu.g8.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.model.IncomingEvent;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.repository.EventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class IncidentKafkaListener {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.incident}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleIncidentEvent(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = objectMapper.readValue(message, Map.class);

            String statut = (String) raw.get("statut");

            // Only process events where statut is CLOTURE or ANNULE
            if (!"CLOTURE".equals(statut) && !"ANNULE".equals(statut)) {
                log.warn("Incident event ignored because status is not final: statut={}", statut);
                return;
            }

            String reference = (String) raw.get("reference");
            String type = (String) raw.get("type");
            String gravite = (String) raw.get("gravite");
            String ligneTransport = (String) raw.get("ligneTransport");

            Object latObj = raw.get("latitude");
            Object lonObj = raw.get("longitude");
            String dateIncidentStr = (String) raw.get("dateIncident");
            String dateResolutionStr = (String) raw.get("dateResolution");

            // Severity mapping
            String severity = mapSeverity(gravite);

            // Zone derivation: "latitude,longitude"
            String zone = null;
            if (latObj != null && lonObj != null) {
                zone = latObj + "," + lonObj;
            }

            // Resolution minutes computation
            Integer resolutionMinutes = computeResolutionMinutes(dateIncidentStr, dateResolutionStr);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("incidentId", reference);
            payload.put("type", type);
            payload.put("severity", severity);
            payload.put("zone", zone);
            payload.put("status", statut);
            payload.put("line", ligneTransport);
            payload.put("resolutionMinutes", resolutionMinutes);

            LocalDateTime eventTimestamp = LocalDateTime.parse(dateIncidentStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            IncomingEvent event = IncomingEvent.builder()
                    .sourceType(SourceType.INCIDENT)
                    .payload(payload)
                    .timestamp(eventTimestamp)
                    .receivedAt(LocalDateTime.now())
                    .processed(false)
                    .build();

            eventRepository.save(event);
            log.info("Incident event received from Kafka: {}, type={}, severity={}, zone={}",
                    reference, type, severity, zone);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse incident Kafka message: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error processing incident Kafka message: {}", e.getMessage(), e);
        }
    }

    private String mapSeverity(String gravite) {
        if (gravite == null) return "LOW";
        return switch (gravite) {
            case "FAIBLE" -> "LOW";
            case "MOYEN" -> "MEDIUM";
            case "ELEVE" -> "HIGH";
            case "CRITIQUE" -> "CRITICAL";
            default -> "LOW";
        };
    }

    private Integer computeResolutionMinutes(String dateIncidentStr, String dateResolutionStr) {
        if (dateIncidentStr == null || dateResolutionStr == null) {
            return null;
        }
        try {
            LocalDateTime dateIncident = LocalDateTime.parse(dateIncidentStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime dateResolution = LocalDateTime.parse(dateResolutionStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return (int) Duration.between(dateIncident, dateResolution).toMinutes();
        } catch (DateTimeParseException e) {
            log.warn("Could not parse dates for resolution minutes computation: {}", e.getMessage());
            return null;
        }
    }
}
