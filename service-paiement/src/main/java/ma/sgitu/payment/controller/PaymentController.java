package ma.sgitu.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.sgitu.payment.dto.request.PaymentRequest;
import ma.sgitu.payment.dto.response.PaymentDetailsResponse;
import ma.sgitu.payment.dto.response.PaymentResponse;
import ma.sgitu.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Paiements", description = "Gestion des transactions de paiement")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Créer et traiter un paiement")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        HttpStatus status = "SUCCESS".equals(response.getStatus())
                ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Consulter une transaction par son ID")
    public ResponseEntity<PaymentDetailsResponse> getPaymentById(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Historique des paiements d'un utilisateur")
    public ResponseEntity<List<PaymentDetailsResponse>> getPaymentsByUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId));
    }

    @PutMapping("/{paymentId}/cancel")
    @Operation(summary = "Annuler un paiement PENDING")
    public ResponseEntity<PaymentDetailsResponse> cancelPayment(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.cancelPayment(paymentId));
    }
}