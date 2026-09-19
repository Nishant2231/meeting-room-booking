package com.nishant.meeting_room_booking.controller;

import com.nishant.meeting_room_booking.dto.BookingRequest;
import com.nishant.meeting_room_booking.dto.BookingResponse;
import com.nishant.meeting_room_booking.entity.Booking;
import com.nishant.meeting_room_booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.createBooking(
                request.getRoomId(), request.getUserId(), request.getStartTime(), request.getEndTime());
        return ResponseEntity.ok(BookingResponse.fromEntity(booking));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getBookingsForUser(@RequestParam Long userId) {
        List<BookingResponse> bookings = bookingService.getBookingsForUser(userId)
                .stream()
                .map(BookingResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(bookings);
    }
}