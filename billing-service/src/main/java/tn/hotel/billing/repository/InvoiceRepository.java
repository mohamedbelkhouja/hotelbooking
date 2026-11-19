package tn.hotel.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hotel.billing.entity.InvoiceEntity;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    Optional<InvoiceEntity> findByReservationId(String reservationId);
    Optional<InvoiceEntity> findByInvoiceId(String invoiceId);
}
