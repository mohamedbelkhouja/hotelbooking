package tn.hotel.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {
    private UUID id;
    private UUID clientId;
    private String roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private ReservationStatus status;
    private Double totalPrice;
    private InvoiceStatus invoiceStatus;
    private LocalDateTime createdAt;
}
