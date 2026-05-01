package ma.sgitu.payment.service;

import lombok.RequiredArgsConstructor;
import ma.sgitu.payment.dto.request.PaymentRequest;
import ma.sgitu.payment.dto.response.PaymentDetailsResponse;
import ma.sgitu.payment.dto.response.PaymentResponse;
import ma.sgitu.payment.entity.Payment;
import ma.sgitu.payment.enums.PaymentStatus;
import ma.sgitu.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        // Créer transaction PENDING
        Payment payment = Payment.builder()
                .userId(request.getUserId())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .savedPaymentToken(request.getSavedPaymentToken())
                .status(PaymentStatus.PENDING)
                .transactionToken("PAY-" + System.currentTimeMillis())
                .build();

        payment = paymentRepository.save(payment);

        // La vérification du compte, débit, facture, notification
        // → sera fait par Personne 2 (compte) et Personne 4 (facture/notification)
        // Pour l'instant on retourne PENDING
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .transactionToken(payment.getTransactionToken())
                .status(payment.getStatus().name())
                .message("Paiement en attente de traitement")
                .build();
    }

    public PaymentDetailsResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable: " + paymentId));
        return toDetailsResponse(payment);
    }

    public List<PaymentDetailsResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(this::toDetailsResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentDetailsResponse cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Seul un paiement PENDING peut être annulé");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
        return toDetailsResponse(payment);
    }

    private PaymentDetailsResponse toDetailsResponse(Payment payment) {
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