package ma.sgitu.g5.provider;

import java.util.UUID;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.response.SendResultDTO;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwilioSMSAdapter implements ISMSProvider {

    /**
     * Envoie un SMS via Twilio (simulé en dev).
     * Protégé par un Circuit Breaker "notificationProvider" :
     * si le taux d'échec dépasse 50% sur 10 appels, le circuit s'ouvre 30s
     * et la méthode fallback est appelée immédiatement.
     *
     * @param phone   numéro de téléphone du destinataire
     * @param message contenu du SMS
     * @return {@link SendResultDTO} avec le résultat de l'envoi
     */
    @Override
    @CircuitBreaker(name = "notificationProvider", fallbackMethod = "sendFallback")
    public SendResultDTO send(String phone, String message) {
        if (phone == null || phone.isBlank()) {
            SendResultDTO result = new SendResultDTO();
            result.setSuccess(false);
            result.setErrorCode("PHONE_MISSING");
            result.setRetryCount(0);
            return result;
        }

        log.info("[G5-SMS] Simule -> {} | {}", phone, message);

        SendResultDTO result = new SendResultDTO();
        result.setSuccess(true);
        result.setProvider("twilio-mock-" + UUID.randomUUID());
        result.setRetryCount(0);
        return result;
    }

    /**
     * Fallback déclenché par le Circuit Breaker lorsque le circuit est OPEN
     * ou lors d'une exception non rattrapée dans {@link #send}.
     * Retourne un {@link SendResultDTO} d'échec afin que
     * {@code NotificationServiceImpl.handleFailure()} prenne le relais.
     */
    public SendResultDTO sendFallback(String phone, String message, Throwable ex) {
        log.warn("[G5-SMS] Circuit Breaker actif — fallback pour {} | cause: {}", phone, ex.getMessage());
        SendResultDTO result = new SendResultDTO();
        result.setSuccess(false);
        result.setErrorCode("CIRCUIT_BREAKER_SMS_OPEN");
        result.setRetryCount(0);
        return result;
    }
}
