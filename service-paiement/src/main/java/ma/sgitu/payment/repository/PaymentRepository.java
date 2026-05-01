package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.Payment;
import ma.sgitu.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);
    Optional<Payment> findByTransactionToken(String transactionToken);
    boolean existsBySourceTypeAndSourceId(String sourceType, Long sourceId);
}