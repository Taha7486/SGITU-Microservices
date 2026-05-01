package ma.sgitu.payment.mapper;

import ma.sgitu.payment.dto.response.PaymentDetailsResponse;
import ma.sgitu.payment.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentDetailsResponse toDetailsResponse(Payment payment) {
        return PaymentDetailsResponse.builder()
                .id(payment.getId())
                .transactionToken(payment.getTransactionToken())
                .userId(payment.getUserId())
                .sourceType(payment.getSourceType().name())
                .sourceId(payment.getSourceId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().name())
                .savedPaymentToken(payment.getSavedPaymentToken())
                .status(payment.getStatus().name())
                .failureReason(payment.getFailureReason() != null ? payment.getFailureReason().name() : null)
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}