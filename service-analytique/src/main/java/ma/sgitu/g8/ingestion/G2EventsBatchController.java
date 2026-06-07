package ma.sgitu.g8.ingestion;

import com.mongodb.MongoException;
import lombok.RequiredArgsConstructor;
import ma.sgitu.g8.ingestion.dto.BatchIngestionResponse;
import ma.sgitu.g8.model.SourceType;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compatibility endpoint for G2 abonnement-service ({@code AnalyseClient}).
 * G2 batches {@link G2AnalyseEventRequest} rows to {@code POST /api/events/batch}.
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class G2EventsBatchController {

    private final IngestionService ingestionService;

    @PostMapping("/batch")
    public ResponseEntity<BatchIngestionResponse> ingestG2Batch(@RequestBody List<G2AnalyseEventRequest> events) {
        if (events == null || events.isEmpty()) {
            return ResponseEntity.badRequest().body(BatchIngestionResponse.builder()
                    .totalReceived(0)
                    .totalAccepted(0)
                    .totalRejected(0)
                    .rejectedReasons(List.of("Request body must contain at least one event."))
                    .status("REJECTED")
                    .build());
        }

        List<Map<String, Object>> rawEvents = new ArrayList<>();
        for (G2AnalyseEventRequest event : events) {
            rawEvents.add(toRawEvent(event));
        }

        try {
            BatchIngestionResponse response = ingestionService.ingest(rawEvents, SourceType.SUBSCRIPTION);
            return ResponseEntity.status(resolveStatus(response)).body(response);
        } catch (DataAccessException | MongoException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    private Map<String, Object> toRawEvent(G2AnalyseEventRequest event) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("schemaVersion", 1);
        raw.put("timestamp", resolveTimestamp(event.getTimestamp()));
        raw.put("userId", normalizeUserId(event.getUserId()));
        raw.put("action", event.getAction());
        if (event.getPlanType() != null && !event.getPlanType().isBlank()) {
            raw.put("planType", event.getPlanType());
        }
        return raw;
    }

    private String normalizeUserId(String userId) {
        if (userId == null) {
            return null;
        }
        return userId.startsWith("USR-") ? userId.substring(4) : userId;
    }

    private String resolveTimestamp(String timestamp) {
        if (timestamp != null && !timestamp.isBlank()) {
            return timestamp;
        }
        return OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private HttpStatus resolveStatus(BatchIngestionResponse response) {
        return switch (response.getStatus()) {
            case "SUCCESS" -> HttpStatus.CREATED;
            case "PARTIAL" -> HttpStatus.MULTI_STATUS;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
