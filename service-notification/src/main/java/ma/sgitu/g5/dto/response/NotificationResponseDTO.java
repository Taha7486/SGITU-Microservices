package ma.sgitu.g5.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Réponse immédiate après réception (202 Accepted)")
public class NotificationResponseDTO {

    @Schema(example = "uuid-g4-001")
    private String notificationId;

    @Schema(example = "QUEUED", description = "QUEUED | ALREADY_QUEUED | ERROR")
    private String status;

    @Schema(example = "Notification prise en charge")
    private String message;

    @Schema(example = "EMAIL")
    private String channel;

    @Schema(example = "2026-05-04T20:30:00Z")
    private String queuedAt;
}