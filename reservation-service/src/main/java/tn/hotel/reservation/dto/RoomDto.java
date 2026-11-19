package tn.hotel.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {
    private String id;
    private String number;
    private RoomType type;
    private Integer capacity;
    private Double pricePerNight;
}
