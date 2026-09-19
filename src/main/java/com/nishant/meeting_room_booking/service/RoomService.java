package com.nishant.meeting_room_booking.service;

import com.nishant.meeting_room_booking.entity.Room;
import com.nishant.meeting_room_booking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public Room createRoom(String name, int capacity, String location) {
        Room room = new Room();
        room.setName(name);
        room.setCapacity(capacity);
        room.setLocation(location);
        return roomRepository.save(room);
    }

    public List<Room> findAvailableRooms(LocalDateTime startTime, LocalDateTime endTime) {
        return roomRepository.findAvailableRooms(startTime, endTime);
    }
}