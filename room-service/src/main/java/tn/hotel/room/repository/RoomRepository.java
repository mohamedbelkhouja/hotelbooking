package tn.hotel.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.hotel.room.entity.RoomEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, String> {

    Optional<RoomEntity> findByNumber(String number);

    @Query("""
        SELECT r FROM RoomEntity r
        WHERE (:type IS NULL OR :type = '' OR r.type = :type)
          AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
          AND r.id NOT IN (
              SELECT b.roomId FROM RoomBookingEntity b
              WHERE b.status = 'ACTIVE'
                AND b.checkIn < :checkOut
                AND b.checkOut > :checkIn
          )
        ORDER BY r.pricePerNight ASC
    """)
    List<RoomEntity> findAvailableRooms(
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("type") String type,
            @Param("minCapacity") Integer minCapacity
    );
}
