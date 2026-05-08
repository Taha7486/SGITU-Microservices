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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class VehiculeKafkaListener {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.vehicule}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleVehiculeEvent(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = objectMapper.readValue(message, Map.class);

            String vehicleId = (String) raw.get("vehicleId");
            String line = (String) raw.get("line");
            String status = (String) raw.get("status");
            Object speedObj = raw.get("speed");
            Object delayObj = raw.get("delayMinutes");
            String timestamp = (String) raw.get("timestamp");

            Double speed = speedObj != null ? ((Number) speedObj).doubleValue() : null;
            Integer delayMinutes = delayObj != null ? ((Number) delayObj).intValue() : null;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("vehicleId", vehicleId);
            payload.put("line", line);
            payload.put("status", status);
            payload.put("speed", speed);
            payload.put("delayMinutes", delayMinutes);

            LocalDateTime eventTimestamp = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            IncomingEvent event = IncomingEvent.builder()
                    .sourceType(SourceType.VEHICLE)
                    .sourceId(vehicleId)
                    .eventType("VEHICLE_IN_SERVICE")
                    .lineId(line)
                    .payload(payload)
                    .timestamp(eventTimestamp)
                    .receivedAt(LocalDateTime.now())
                    .processed(false)
                    .build();

            eventRepository.save(event);
            log.info("Vehicle event received from Kafka: {}", vehicleId);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse vehicle Kafka message: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error processing vehicle Kafka message: {}", e.getMessage(), e);
        }
    }
}
