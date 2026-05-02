package com.serviceabonnement.client;

import com.serviceabonnement.dto.external.PaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "service-paiement", url = "${application.config.paiement-service-url}")
public interface PaiementClient {

    @PostMapping("/api/v1/payments/process")
    PaymentResponseDTO processPayment(@RequestParam("abonnementId") Long abonnementId, 
                                     @RequestParam("montant") BigDecimal montant);
}
