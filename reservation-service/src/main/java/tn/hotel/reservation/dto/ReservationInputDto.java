package tn.hotel.reservation.dto;

import jakarta.validation.constraints.NotNull;
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
public class ReservationInputDto {

    @NotNull(message = "clientId is required")
    private UUID clientId;

    @NotNull(message = "roomId is required")
    private String roomId;

    @NotNull(message = "checkIn is required")
    private LocalDate checkIn;

    @NotNull(message = "checkOut is required")
    private LocalDate checkOut;
}
