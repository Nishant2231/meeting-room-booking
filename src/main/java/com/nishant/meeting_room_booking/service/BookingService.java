package com.nishant.meeting_room_booking.service;

import com.nishant.meeting_room_booking.entity.Booking;
import com.nishant.meeting_room_booking.entity.Room;
import com.nishant.meeting_room_booking.entity.AppUser;
import com.nishant.meeting_room_booking.exception.BookingConflictException;
import com.nishant.meeting_room_booking.exception.RoomNotFoundException;
import com.nishant.meeting_room_booking.repository.BookingRepository;
import com.nishant.meeting_room_booking.repository.RoomRepository;
import com.nishant.meeting_room_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional
    public Booking createBooking(Long roomId, Long userId, LocalDateTime startTime, LocalDateTime endTime) {

        // --- Basic validation, before we even touch locking ---
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book a slot in the past");
        }

        // --- Step 1: acquire the lock on this room ---
        // Everything below this line is protected: no other transaction
        // can run this same block for the same room until we commit or rollback.
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // --- Step 2: check for conflicts, now that we hold the lock ---
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(roomId, startTime, endTime);
        if (!overlapping.isEmpty()) {
            throw new BookingConflictException(
                    "Room " + room.getName() + " is already booked for an overlapping time slot"
            );
        }

        // --- Step 3: safe to insert ---
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setUser(user);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        return bookingRepository.save(booking);
        // Lock is released automatically when this method returns and
        // the @Transactional wrapper commits.
    }

    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public List<Booking> getBookingsForUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }
}