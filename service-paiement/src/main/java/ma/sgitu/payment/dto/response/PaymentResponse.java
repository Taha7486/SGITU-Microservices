package ma.sgitu.payment.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private Long paymentId;
    private String transactionToken;
    private String status;
    private String message;
    private Long invoiceId;
    private String invoiceNumber;
    private String failureReason;
}