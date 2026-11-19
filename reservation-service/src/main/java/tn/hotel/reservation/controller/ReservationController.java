package tn.hotel.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.hotel.reservation.dto.InvoiceDto;
import tn.hotel.reservation.dto.ReservationDto;
import tn.hotel.reservation.dto.ReservationInputDto;
import tn.hotel.reservation.service.ReservationService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Gestion du cycle de vie des réservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Créer une réservation (vérifie la disponibilité via gRPC) - statut CREEE")
    public ResponseEntity<ReservationDto> createReservation(@Valid @RequestBody ReservationInputDto input) {
        ReservationDto created = reservationService.createReservation(input);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Consulter une réservation")
    public ResponseEntity<ReservationDto> getReservation(@PathVariable("reservationId") UUID reservationId) {
        return ResponseEntity.ok(reservationService.getReservation(reservationId));
    }

    @PutMapping("/{reservationId}")
    @Operation(summary = "Modifier dates / chambre (statut CREEE uniquement)")
    public ResponseEntity<ReservationDto> updateReservation(
            @PathVariable("reservationId") UUID reservationId,
            @Valid @RequestBody ReservationInputDto input) {
        return ResponseEntity.ok(reservationService.updateReservation(reservationId, input));
    }

    @DeleteMapping("/{reservationId}")
    @Operation(summary = "Annuler une réservation (suppression logique, statut ANNULEE)")
    public ResponseEntity<ReservationDto> cancelReservation(@PathVariable("reservationId") UUID reservationId) {
        return ResponseEntity.ok(reservationService.cancelReservation(reservationId));
    }

    @PostMapping("/{reservationId}/confirm")
    @Operation(summary = "Confirmer une réservation et déclencher la facturation SOAP (circuit breaker)")
    public ResponseEntity<ReservationDto> confirmReservation(@PathVariable("reservationId") UUID reservationId) {
        return ResponseEntity.ok(reservationService.confirmReservation(reservationId));
    }

    @GetMapping("/{reservationId}/invoice")
    @Operation(summary = "Obtenir la facture d'une réservation (délègue à billing-service SOAP)")
    public ResponseEntity<InvoiceDto> getReservationInvoice(@PathVariable("reservationId") UUID reservationId) {
        return ResponseEntity.ok(reservationService.getReservationInvoice(reservationId));
    }
}
