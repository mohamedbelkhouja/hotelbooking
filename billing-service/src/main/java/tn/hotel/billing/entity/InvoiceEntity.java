package tn.hotel.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false, unique = true, length = 64)
    private String invoiceId;

    @Column(name = "reservation_id", nullable = false, length = 36)
    private String reservationId;

    @Column(name = "client_id", nullable = false, length = 36)
    private String clientId;

    @Column(name = "amount_ht", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountHT;

    @Column(name = "tva", nullable = false, precision = 10, scale = 2)
    private BigDecimal tva;

    @Column(name = "amount_ttc", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountTTC;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
