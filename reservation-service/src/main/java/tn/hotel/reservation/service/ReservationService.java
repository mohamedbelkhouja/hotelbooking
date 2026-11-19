package tn.hotel.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hotel.reservation.client.BillingSoapClient;
import tn.hotel.reservation.client.RoomGrpcClient;
import tn.hotel.reservation.dto.*;
import tn.hotel.reservation.entity.ReservationEntity;
import tn.hotel.reservation.exception.BadRequestException;
import tn.hotel.reservation.exception.ConflictException;
import tn.hotel.reservation.exception.ResourceNotFoundException;
import tn.hotel.reservation.repository.ClientRepository;
import tn.hotel.reservation.repository.ReservationRepository;
import tn.hotel.room.v1.CheckAvailabilityResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ClientRepository clientRepository;
    private final RoomGrpcClient roomGrpcClient;
    private final BillingSoapClient billingSoapClient;

    @Transactional
    public ReservationDto createReservation(ReservationInputDto input) {
        validateDates(input);

        if (!clientRepository.existsById(input.getClientId())) {
            throw new ResourceNotFoundException("Client not found with ID: " + input.getClientId());
        }

        // Check availability via gRPC with deadline + retry
        CheckAvailabilityResponse availability = roomGrpcClient.checkAvailability(
                input.getRoomId(), input.getCheckIn(), input.getCheckOut());

        if (!availability.getAvailable()) {
            throw new ConflictException("Room is not available for requested period: " +
                    (availability.getReason().isEmpty() ? "already booked" : availability.getReason()));
        }

        long nights = ChronoUnit.DAYS.between(input.getCheckIn(), input.getCheckOut());
        BigDecimal pricePerNight = BigDecimal.valueOf(availability.getPricePerNight());
        BigDecimal totalPrice = pricePerNight.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);

        ReservationEntity entity = ReservationEntity.builder()
                .clientId(input.getClientId())
                .roomId(input.getRoomId())
                .checkIn(input.getCheckIn())
                .checkOut(input.getCheckOut())
                .status(ReservationStatus.CREEE.name())
                .totalPrice(totalPrice)
                .invoiceStatus(InvoiceStatus.EN_ATTENTE.name())
                .build();

        ReservationEntity saved = reservationRepository.save(entity);
        log.info("Created reservation {} for client {}", saved.getId(), saved.getClientId());
        return toReservationDto(saved);
    }

    @Transactional(readOnly = true)
    public ReservationDto getReservation(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .map(ReservationService::toReservationDto)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));
    }

    @Transactional
    public ReservationDto updateReservation(UUID reservationId, ReservationInputDto input) {
        validateDates(input);

        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        if (!ReservationStatus.CREEE.name().equals(reservation.getStatus())) {
            throw new ConflictException("Reservation can only be modified when in status CREEE. Current status: " + reservation.getStatus());
        }

        if (!clientRepository.existsById(input.getClientId())) {
            throw new ResourceNotFoundException("Client not found with ID: " + input.getClientId());
        }

        // Re-check room availability via gRPC
        CheckAvailabilityResponse availability = roomGrpcClient.checkAvailability(
                input.getRoomId(), input.getCheckIn(), input.getCheckOut());

        if (!availability.getAvailable()) {
            throw new ConflictException("Room is not available for updated period: " + availability.getReason());
        }

        long nights = ChronoUnit.DAYS.between(input.getCheckIn(), input.getCheckOut());
        BigDecimal pricePerNight = BigDecimal.valueOf(availability.getPricePerNight());
        BigDecimal totalPrice = pricePerNight.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);

        reservation.setClientId(input.getClientId());
        reservation.setRoomId(input.getRoomId());
        reservation.setCheckIn(input.getCheckIn());
        reservation.setCheckOut(input.getCheckOut());
        reservation.setTotalPrice(totalPrice);

        ReservationEntity updated = reservationRepository.save(reservation);
        log.info("Updated reservation {}", updated.getId());
        return toReservationDto(updated);
    }

    @Transactional
    public ReservationDto cancelReservation(UUID reservationId) {
        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        String currentStatus = reservation.getStatus();
        // Permitted transitions: CREEE -> ANNULEE or CONFIRMEE -> ANNULEE
        if (!ReservationStatus.CREEE.name().equals(currentStatus) &&
                !ReservationStatus.CONFIRMEE.name().equals(currentStatus)) {
            throw new ConflictException("Cannot cancel reservation in status: " + currentStatus);
        }

        reservation.setStatus(ReservationStatus.ANNULEE.name());
        ReservationEntity updated = reservationRepository.save(reservation);
        log.info("Cancelled reservation {}", updated.getId());
        return toReservationDto(updated);
    }

    @Transactional
    public ReservationDto confirmReservation(UUID reservationId) {
        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        // Permitted transitions: CREEE -> CONFIRMEE only
        if (!ReservationStatus.CREEE.name().equals(reservation.getStatus())) {
            throw new ConflictException("Only reservations in status CREEE can be confirmed. Current status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CONFIRMEE.name());

        long nights = ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        BigDecimal pricePerNight = reservation.getTotalPrice().divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);

        // Call SOAP billing-service with Resilience4j Circuit Breaker
        try {
            InvoiceDto invoice = billingSoapClient.generateInvoice(
                    reservation.getId().toString(),
                    reservation.getClientId().toString(),
                    (int) nights,
                    pricePerNight
            );

            if (InvoiceStatus.EMISE.equals(invoice.getStatus())) {
                reservation.setInvoiceStatus(InvoiceStatus.EMISE.name());
            } else {
                reservation.setInvoiceStatus(InvoiceStatus.EN_ATTENTE.name());
            }
        } catch (Exception e) {
            log.warn("SOAP invoice generation failed or circuit breaker active. Fallback applied: invoiceStatus=EN_ATTENTE. Reason: {}", e.getMessage());
            reservation.setInvoiceStatus(InvoiceStatus.EN_ATTENTE.name());
        }

        ReservationEntity updated = reservationRepository.save(reservation);
        log.info("Confirmed reservation {} with invoiceStatus {}", updated.getId(), updated.getInvoiceStatus());
        return toReservationDto(updated);
    }

    @Transactional(readOnly = true)
    public InvoiceDto getReservationInvoice(UUID reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new ResourceNotFoundException("Reservation not found with ID: " + reservationId);
        }

        return billingSoapClient.getInvoice(reservationId.toString());
    }

    private void validateDates(ReservationInputDto input) {
        if (input.getCheckIn() == null || input.getCheckOut() == null) {
            throw new BadRequestException("checkIn and checkOut dates are required");
        }
        if (!input.getCheckOut().isAfter(input.getCheckIn())) {
            throw new BadRequestException("checkOut date must be strictly after checkIn date");
        }
    }

    public static ReservationDto toReservationDto(ReservationEntity entity) {
        return ReservationDto.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .roomId(entity.getRoomId())
                .checkIn(entity.getCheckIn())
                .checkOut(entity.getCheckOut())
                .status(ReservationStatus.valueOf(entity.getStatus()))
                .totalPrice(entity.getTotalPrice() != null ? entity.getTotalPrice().doubleValue() : null)
                .invoiceStatus(InvoiceStatus.valueOf(entity.getInvoiceStatus()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
