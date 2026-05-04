package ma.sgitu.g5.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.response.NotificationResponseDTO;
import ma.sgitu.g5.dto.response.SendResultDTO;
import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final ITemplateService templateService;
    private final IChannelRouter channelRouter;
    private final IRetryService retryService;

    // ----------------------------------------------------------------
    // send() — point d'entrée principal
    // ----------------------------------------------------------------

    @Override
    @Transactional
    public NotificationResponseDTO send(NotificationRequestDTO dto) {

        // 1. Déduplication : si notificationId déjà connu → 409 simulé via retour
        // QUEUED
        String notificationId = dto.getNotificationId();
        if (notificationId == null || notificationId.isBlank()) {
            notificationId = UUID.randomUUID().toString();
        }

        if (notificationRepository.existsByNotificationId(notificationId)) {
            log.warn("[G5] Notification déjà existante — idempotence : {}", notificationId);
            return buildResponse(notificationId, "ALREADY_QUEUED",
                    "Notification déjà prise en charge (idempotence)", dto.getChannel());
        }

        // 2. Hydratation du template (subject + message)
        String message = templateService.hydrateMessage(dto.getEventType(), dto.getMetadata());
        String subject = templateService.hydrateSubject(dto.getEventType(), dto.getMetadata());

        // 3. Persistance initiale avec statut PENDING
        Notification entity = buildEntity(dto, notificationId, subject, message);
        notificationRepository.save(entity);

        // 4. Envoi asynchrone
        dispatchAsync(entity, dto, subject, message);

        // 5. Réponse immédiate 202
        log.info("[G5] Notification mise en file : {} | eventType={} | channel={}",
                notificationId, dto.getEventType(), dto.getChannel());

        return buildResponse(notificationId, "QUEUED",
                "Notification prise en charge", dto.getChannel());
    }

    // ----------------------------------------------------------------
    // retry() — relance manuelle d'un FAILED
    // ----------------------------------------------------------------

    @Override
    @Transactional
    public NotificationResponseDTO retry(String notificationId) {

        Notification entity = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification introuvable : " + notificationId));

        if (!"FAILED".equals(entity.getStatus())) {
            log.warn("[G5] Retry refusé — statut actuel={} pour {}", entity.getStatus(), notificationId);
            return buildResponse(notificationId, entity.getStatus(),
                    "Retry non applicable sur ce statut", entity.getChannel());
        }

        entity.setStatus("PENDING");
        entity.setRetryCount(entity.getRetryCount() + 1);
        notificationRepository.save(entity);

        dispatchAsyncFromEntity(entity);

        log.info("[G5] Retry déclenché manuellement pour {}", notificationId);
        return buildResponse(notificationId, "QUEUED",
                "Relance en cours", entity.getChannel());
    }

    // ----------------------------------------------------------------
    // Envoi asynchrone
    // ----------------------------------------------------------------

    /**
     * Appel async après persistance — ne bloque pas le thread HTTP.
     * En cas d'échec, délègue au RetryService (backoff exponentiel).
     */
    @Async
    protected void dispatchAsync(Notification entity,
            NotificationRequestDTO dto,
            String subject,
            String message) {
        try {
            SendResultDTO result = channelRouter.route(dto, subject, message);
            updateStatus(entity, result);
        } catch (Exception ex) {
            log.error("[G5] Échec dispatch initial pour {} : {}", entity.getNotificationId(), ex.getMessage());
            handleFailure(entity, ex.getMessage());
        }
    }

    @Async
    protected void dispatchAsyncFromEntity(Notification entity) {
        try {
            NotificationRequestDTO dto = rebuildDtoFromEntity(entity);
            SendResultDTO result = channelRouter.route(dto, entity.getSubject(), entity.getContent());
            updateStatus(entity, result);
        } catch (Exception ex) {
            log.error("[G5] Échec retry pour {} : {}", entity.getNotificationId(), ex.getMessage());
            handleFailure(entity, ex.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Gestion du statut après envoi
    // ----------------------------------------------------------------

    @Transactional
    protected void updateStatus(Notification entity, SendResultDTO result) {
        if (result.isSuccess()) {
            entity.setStatus("SENT");
            entity.setSentAt(LocalDateTime.now());
            entity.setProvider(result.getProvider());
            log.info("[G5] Notification SENT : {} via {}", entity.getNotificationId(), result.getProvider());
        } else {
            handleFailure(entity, result.getErrorCode());
        }
        notificationRepository.save(entity);
    }

    @Transactional
    protected void handleFailure(Notification entity, String errorReason) {
        boolean willRetry = retryService.shouldRetry(entity.getRetryCount());
        if (willRetry) {
            int delay = retryService.nextDelaySeconds(entity.getRetryCount());
            entity.setStatus("PENDING");
            entity.setRetryCount(entity.getRetryCount() + 1);
            log.warn("[G5] Planification retry #{} dans {}s pour {}",
                    entity.getRetryCount(), delay, entity.getNotificationId());
            notificationRepository.save(entity);
            retryService.scheduleRetry(entity.getNotificationId(), delay);
        } else {
            entity.setStatus("FAILED");
            log.error("[G5] Notification FAILED définitivement : {} — raison : {}",
                    entity.getNotificationId(), errorReason);
            notificationRepository.save(entity);
        }
    }

    // ----------------------------------------------------------------
    // Helpers de construction
    // ----------------------------------------------------------------

    private Notification buildEntity(NotificationRequestDTO dto,
            String notificationId,
            String subject,
            String message) {
        Notification n = new Notification();
        n.setNotificationId(notificationId);
        n.setSourceService(dto.getSourceService());
        n.setEventType(dto.getEventType());
        n.setChannel(dto.getChannel());
        n.setPriority(dto.getPriority() != null ? dto.getPriority() : "NORMAL");
        n.setUserId(dto.getRecipient() != null ? dto.getRecipient().getUserId() : null);
        n.setEmail(dto.getRecipient() != null ? dto.getRecipient().getEmail() : null);
        n.setPhone(dto.getRecipient() != null ? dto.getRecipient().getPhone() : null);
        n.setDeviceToken(dto.getRecipient() != null ? dto.getRecipient().getDeviceToken() : null);
        n.setSubject(subject);
        n.setContent(message);
        n.setStatus("PENDING");
        n.setRetryCount(0);
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }

    /**
     * Reconstruit un DTO minimal à partir de l'entité pour le retry.
     * Les metadata ne sont pas re-hydratées (template déjà résolu en BDD).
     */
    private NotificationRequestDTO rebuildDtoFromEntity(Notification entity) {
        NotificationRequestDTO dto = new NotificationRequestDTO();
        dto.setNotificationId(entity.getNotificationId());
        dto.setSourceService(entity.getSourceService());
        dto.setEventType(entity.getEventType());
        dto.setChannel(entity.getChannel());
        dto.setPriority(entity.getPriority());

        ma.sgitu.g5.dto.request.RecipientDTO recipient = new ma.sgitu.g5.dto.request.RecipientDTO();
        recipient.setUserId(entity.getUserId());
        recipient.setEmail(entity.getEmail());
        recipient.setPhone(entity.getPhone());
        recipient.setDeviceToken(entity.getDeviceToken());
        dto.setRecipient(recipient);

        // metadata null : le contenu est déjà résolu dans entity.content
        dto.setMetadata(null);
        return dto;
    }

    private NotificationResponseDTO buildResponse(String notificationId,
            String status,
            String message,
            String channel) {
        NotificationResponseDTO resp = new NotificationResponseDTO();
        resp.setNotificationId(notificationId);
        resp.setStatus(status);
        resp.setMessage(message);
        resp.setChannel(channel);
        return resp;
    }
}