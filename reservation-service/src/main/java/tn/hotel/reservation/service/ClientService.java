package tn.hotel.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hotel.reservation.dto.ClientDto;
import tn.hotel.reservation.dto.ClientInputDto;
import tn.hotel.reservation.dto.ReservationDto;
import tn.hotel.reservation.dto.ReservationStatus;
import tn.hotel.reservation.entity.ClientEntity;
import tn.hotel.reservation.entity.ReservationEntity;
import tn.hotel.reservation.exception.ConflictException;
import tn.hotel.reservation.exception.ResourceNotFoundException;
import tn.hotel.reservation.repository.ClientRepository;
import tn.hotel.reservation.repository.ReservationRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public ClientDto createClient(ClientInputDto input) {
        if (clientRepository.existsByEmail(input.getEmail())) {
            throw new ConflictException("A client with email '" + input.getEmail() + "' already exists");
        }

        ClientEntity entity = ClientEntity.builder()
                .firstName(input.getFirstName())
                .lastName(input.getLastName())
                .email(input.getEmail())
                .phone(input.getPhone())
                .build();

        ClientEntity saved = clientRepository.save(entity);
        log.info("Created client with ID: {}", saved.getId());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ClientDto getClient(UUID clientId) {
        return clientRepository.findById(clientId)
                .map(ClientService::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientId));
    }

    @Transactional
    public ClientDto updateClient(UUID clientId, ClientInputDto input) {
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientId));

        if (!client.getEmail().equalsIgnoreCase(input.getEmail()) &&
                clientRepository.existsByEmail(input.getEmail())) {
            throw new ConflictException("A client with email '" + input.getEmail() + "' already exists");
        }

        client.setFirstName(input.getFirstName());
        client.setLastName(input.getLastName());
        client.setEmail(input.getEmail());
        client.setPhone(input.getPhone());

        ClientEntity updated = clientRepository.save(client);
        log.info("Updated client with ID: {}", updated.getId());
        return toDto(updated);
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getClientReservations(UUID clientId, ReservationStatus status) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found with ID: " + clientId);
        }

        List<ReservationEntity> list;
        if (status != null) {
            list = reservationRepository.findByClientIdAndStatus(clientId, status.name());
        } else {
            list = reservationRepository.findByClientId(clientId);
        }

        return list.stream()
                .map(ReservationService::toReservationDto)
                .toList();
    }

    public static ClientDto toDto(ClientEntity entity) {
        return ClientDto.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .build();
    }
}
