package ma.sgitu.g8.kafka;

// DEV/TEST ONLY — this controller is excluded from the "prod" Spring profile.
// Set SPRING_PROFILES_ACTIVE=prod in production to disable these endpoints.

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/test/kafka")
@RequiredArgsConstructor
@Profile("!prod")
public class KafkaTestController {

    private final VehiculeKafkaListener vehiculeKafkaListener;
    private final IncidentKafkaListener incidentKafkaListener;

    @PostMapping("/vehicule")
    public ResponseEntity<String> simulateVehiculeEvent(@RequestBody String payload) {
        try {
            log.info("Simulating vehicle Kafka event");
            vehiculeKafkaListener.handleVehiculeEvent(payload);
            return ResponseEntity.ok("Vehicle Kafka event simulated successfully");
        } catch (Exception e) {
            log.error("Error simulating vehicle Kafka event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/incident")
    public ResponseEntity<String> simulateIncidentEvent(@RequestBody String payload) {
        try {
            log.info("Simulating incident Kafka event");
            incidentKafkaListener.handleIncidentEvent(payload);
            return ResponseEntity.ok("Incident Kafka event simulated successfully");
        } catch (Exception e) {
            log.error("Error simulating incident Kafka event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
