package tn.hotel.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hotel.reservation.entity.ReservationEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {
    List<ReservationEntity> findByClientId(UUID clientId);
    List<ReservationEntity> findByClientIdAndStatus(UUID clientId, String status);
}
