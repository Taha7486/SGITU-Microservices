package ma.sgitu.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import ma.sgitu.payment.enums.PaymentMethod;
import ma.sgitu.payment.enums.SourceType;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotNull(message = "userId est obligatoire")
    private Long userId;

    @NotNull(message = "sourceType est obligatoire")
    private SourceType sourceType;

    @NotNull(message = "sourceId est obligatoire")
    private Long sourceId;

    @NotNull(message = "amount est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal amount;

    @NotNull(message = "paymentMethod est obligatoire")
    private PaymentMethod paymentMethod;

    @NotNull(message = "savedPaymentToken est obligatoire")
    private String savedPaymentToken;
}