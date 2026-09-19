package com.nishant.meeting_room_booking.repository;

import com.nishant.meeting_room_booking.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(Long id);
    @Query("""
    SELECT r FROM Room r WHERE r.id NOT IN (
        SELECT b.room.id FROM Booking b
        WHERE b.status = 'CONFIRMED'
        AND b.startTime < :endTime
        AND b.endTime > :startTime
    )
""")
    List<Room> findAvailableRooms(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}