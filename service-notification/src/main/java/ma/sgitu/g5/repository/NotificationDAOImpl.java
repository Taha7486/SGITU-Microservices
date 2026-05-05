package ma.sgitu.g5.repository;

import lombok.RequiredArgsConstructor;
import ma.sgitu.g5.entity.Notification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationDAOImpl implements INotificationDAO {

    private final NotificationRepository notificationRepository;

    @Override
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public Optional<Notification> findByNotificationId(String notificationId) {
        return notificationRepository.findByNotificationId(notificationId);
    }

    @Override
    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    @Override
    public boolean existsByNotificationId(String notificationId) {
        return notificationRepository.existsByNotificationId(notificationId);
    }
}
