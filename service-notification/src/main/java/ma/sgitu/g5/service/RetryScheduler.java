package ma.sgitu.g5.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.entity.NotificationStatus;
import ma.sgitu.g5.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduler de retry automatique des notifications.
 * <p>
 * S'exécute toutes les 5 minutes et relance toutes les notifications
 * dont le statut est {@link NotificationStatus#FAILED} et dont le nombre
 * de tentatives est inférieur à {@value #MAX_RETRY_COUNT}.
 * Les notifications éligibles sont remises à {@link NotificationStatus#PENDING}
 * puis redispatchées de manière asynchrone via
 * {@link NotificationServiceImpl#dispatchAsyncFromEntity(Notification)}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    /** Nombre maximum de tentatives avant abandon définitif. */
    private static final int MAX_RETRY_COUNT = 3;

    private final NotificationRepository notificationRepository;
    private final NotificationServiceImpl notificationService;

    /**
     * Tâche planifiée : toutes les 5 minutes (300 000 ms).
     * Recherche les notifications FAILED éligibles, les remet en PENDING
     * et relance l'envoi asynchrone.
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void retryFailedNotifications() {
        List<Notification> failedNotifications = notificationRepository
                .findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, MAX_RETRY_COUNT);

        if (failedNotifications.isEmpty()) {
            log.debug("[RetryScheduler] Aucune notification FAILED éligible au retry.");
            return;
        }

        log.info("[RetryScheduler] {} notification(s) FAILED trouvée(s) — relance en cours...",
                failedNotifications.size());

        for (Notification notification : failedNotifications) {
            try {
                log.info("[RetryScheduler] Relance de la notification id={} (retryCount={}/{})",
                        notification.getNotificationId(),
                        notification.getRetryCount(),
                        MAX_RETRY_COUNT);

                // Remettre le statut à PENDING avant le redispatch
                notification.setStatus(NotificationStatus.PENDING);
                notificationRepository.save(notification);

                // Relancer l'envoi de manière asynchrone
                notificationService.dispatchAsyncFromEntity(notification);

            } catch (Exception ex) {
                log.error("[RetryScheduler] Erreur lors de la relance de la notification id={} : {}",
                        notification.getNotificationId(), ex.getMessage(), ex);
            }
        }

        log.info("[RetryScheduler] Cycle de retry terminé pour {} notification(s).",
                failedNotifications.size());
    }
}
