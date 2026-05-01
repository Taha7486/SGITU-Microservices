package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.PaymentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, Long> {
    Optional<PaymentAccount> findByPaymentToken(String token);
}