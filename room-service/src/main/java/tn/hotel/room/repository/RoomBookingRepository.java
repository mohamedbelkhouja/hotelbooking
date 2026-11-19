package tn.hotel.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.hotel.room.entity.RoomBookingEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomBookingRepository extends JpaRepository<RoomBookingEntity, String> {

    @Query("""
        SELECT b FROM RoomBookingEntity b
        WHERE b.roomId = :roomId
          AND b.status = 'ACTIVE'
          AND b.checkIn < :checkOut
          AND b.checkOut > :checkIn
    """)
    List<RoomBookingEntity> findOverlappingBookings(
            @Param("roomId") String roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );
}
