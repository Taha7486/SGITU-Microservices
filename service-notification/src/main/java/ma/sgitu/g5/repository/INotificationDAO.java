package ma.sgitu.g5.repository;

import ma.sgitu.g5.entity.Notification;
import java.util.Optional;
import java.util.List;

public interface INotificationDAO {
    Notification save(Notification notification);
    Optional<Notification> findByNotificationId(String notificationId);
    List<Notification> findAll();
    boolean existsByNotificationId(String notificationId);
}
