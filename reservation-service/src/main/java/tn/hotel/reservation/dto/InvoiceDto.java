package tn.hotel.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private String invoiceId;
    private UUID reservationId;
    private Double amountHT;
    private Double tva;
    private Double amountTTC;
    private LocalDate issueDate;
    private InvoiceStatus status;
}
