package tn.hotel.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.hotel.reservation.dto.ClientDto;
import tn.hotel.reservation.dto.ClientInputDto;
import tn.hotel.reservation.dto.ReservationDto;
import tn.hotel.reservation.dto.ReservationStatus;
import tn.hotel.reservation.service.ClientService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Gestion des clients")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @Operation(summary = "Créer un client")
    public ResponseEntity<ClientDto> createClient(@Valid @RequestBody ClientInputDto input) {
        ClientDto created = clientService.createClient(input);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{clientId}")
    @Operation(summary = "Consulter un client")
    public ResponseEntity<ClientDto> getClient(@PathVariable("clientId") UUID clientId) {
        return ResponseEntity.ok(clientService.getClient(clientId));
    }

    @PutMapping("/{clientId}")
    @Operation(summary = "Modifier un client")
    public ResponseEntity<ClientDto> updateClient(
            @PathVariable("clientId") UUID clientId,
            @Valid @RequestBody ClientInputDto input) {
        return ResponseEntity.ok(clientService.updateClient(clientId, input));
    }

    @GetMapping("/{clientId}/reservations")
    @Operation(summary = "Suivre les réservations d'un client")
    public ResponseEntity<List<ReservationDto>> listClientReservations(
            @PathVariable("clientId") UUID clientId,
            @RequestParam(name = "status", required = false) ReservationStatus status) {
        return ResponseEntity.ok(clientService.getClientReservations(clientId, status));
    }
}
