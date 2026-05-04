package ma.sgitu.g5.service;

import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.response.NotificationResponseDTO;

public interface INotificationService {

    /**
     * Point d'entrée principal — reçu depuis REST (G4, G6, G8, G9, G10)
     * ou construit par KafkaConsumerController (G1, G2, G3).
     *
     * Flux :
     * 1. Déduplication sur notificationId
     * 2. Validation du canal / recipient
     * 3. Hydratation du template via ITemplateService
     * 4. Persistance en BDD avec statut PENDING
     * 5. Routage vers le bon provider via IChannelRouter (async)
     * 6. Retourne immédiatement 202 Accepted
     *
     * @param dto le payload normalisé (venant REST ou construit depuis Kafka)
     * @return réponse 202 avec notificationId + status QUEUED
     */
    NotificationResponseDTO send(NotificationRequestDTO dto);

    /**
     * Relance manuelle d'une notification en échec (FAILED).
     * Appelé par POST /api/notifications/{notificationId}/retry
     *
     * @param notificationId UUID de la notification à relancer
     * @return résultat de la relance
     */
    NotificationResponseDTO retry(String notificationId);
}