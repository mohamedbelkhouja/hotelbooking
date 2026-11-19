package tn.hotel.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hotel.reservation.client.RoomGrpcClient;
import tn.hotel.reservation.dto.RoomDto;
import tn.hotel.reservation.dto.RoomType;
import tn.hotel.reservation.exception.BadRequestException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Consultation des chambres via gRPC")
public class RoomController {

    private final RoomGrpcClient roomGrpcClient;

    @GetMapping("/available")
    @Operation(summary = "Consulter les chambres disponibles sur une période")
    public ResponseEntity<List<RoomDto>> listAvailableRooms(
            @RequestParam("checkIn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam("checkOut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(name = "type", required = false) RoomType type,
            @RequestParam(name = "minCapacity", required = false) Integer minCapacity) {

        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("checkOut date must be strictly after checkIn date");
        }

        List<RoomDto> rooms = roomGrpcClient.streamAvailableRooms(checkIn, checkOut, type, minCapacity);
        return ResponseEntity.ok(rooms);
    }
}
