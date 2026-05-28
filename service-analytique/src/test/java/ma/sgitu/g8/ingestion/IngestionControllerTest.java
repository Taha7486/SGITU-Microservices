package ma.sgitu.g8.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.sgitu.g8.ingestion.dto.BatchIngestionResponse;
import ma.sgitu.g8.model.SourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngestionController.class)
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IngestionService ingestionService;

    @Test
    @DisplayName("POST /api/v1/ingestion/tickets returns 201 when the batch is successful")
    void ticketsBatchSuccess() throws Exception {
        when(ingestionService.ingest(anyList(), eq(SourceType.TICKETING)))
                .thenReturn(response("SUCCESS", 1, 1, 0));

        mockMvc.perform(post("/api/v1/ingestion/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(Map.of(
                                "timestamp", "2026-05-05T11:00:00Z",
                                "userId", "user-1",
                                "status", "validated"
                        )))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalAccepted").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/ingestion/payments returns 207 when the batch is partial")
    void paymentsBatchPartial() throws Exception {
        when(ingestionService.ingest(anyList(), eq(SourceType.PAYMENT)))
                .thenReturn(response("PARTIAL", 3, 2, 1));

        mockMvc.perform(post("/api/v1/ingestion/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(
                                Map.of("timestamp", "2026-05-05T11:00:00Z", "transactionId", "tx-1", "status", "completed"),
                                Map.of("garbage", "data"),
                                Map.of("timestamp", "2026-05-05T11:01:00Z", "transactionId", "tx-2", "status", "completed")
                        ))))
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.totalRejected").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/ingestion/users returns 400 for an empty batch")
    void usersBatchEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    private BatchIngestionResponse response(String status, int received, int accepted, int rejected) {
        return BatchIngestionResponse.builder()
                .status(status)
                .totalReceived(received)
                .totalAccepted(accepted)
                .totalRejected(rejected)
                .rejectedReasons(rejected == 0 ? List.of() : List.of("Rejected event"))
                .build();
    }
}
