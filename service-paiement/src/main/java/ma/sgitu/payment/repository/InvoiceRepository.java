package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}