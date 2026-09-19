package com.nishant.meeting_room_booking;

import com.nishant.meeting_room_booking.entity.AppUser;
import com.nishant.meeting_room_booking.entity.Room;
import com.nishant.meeting_room_booking.exception.BookingConflictException;
import com.nishant.meeting_room_booking.repository.RoomRepository;
import com.nishant.meeting_room_booking.repository.UserRepository;
import com.nishant.meeting_room_booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    private Long roomId;
    private Long userId;

    @BeforeEach
    void setUp() {
        Room room = new Room();
        room.setName("Concurrency Test Room");
        room.setCapacity(5);
        room = roomRepository.save(room);
        roomId = room.getId();

        AppUser user = new AppUser();
        user.setName("Concurrency Tester");
        user.setEmail("concurrency-" + System.nanoTime() + "@test.com");
        user = userRepository.save(user);
        userId = user.getId();
    }

    @Test
    void whenTwoThreadsBookSameSlotSimultaneously_onlyOneSucceeds() throws InterruptedException {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(1);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    bookingService.createBooking(roomId, userId, start, end);
                    successCount.incrementAndGet();

                } catch (BookingConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one booking should succeed");
        assertEquals(1, conflictCount.get(), "Exactly one booking should be rejected as a conflict");
    }
}