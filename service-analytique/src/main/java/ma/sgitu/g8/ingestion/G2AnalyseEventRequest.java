package ma.sgitu.g8.ingestion;

import lombok.Data;

@Data
public class G2AnalyseEventRequest {
    private String timestamp;
    private String userId;
    private String action;
    private String planType;
}
