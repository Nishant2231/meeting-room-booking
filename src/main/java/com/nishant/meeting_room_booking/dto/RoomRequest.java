package com.nishant.meeting_room_booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RoomRequest {

    @NotBlank(message = "name is required")
    private String name;

    @Positive(message = "capacity must be positive")
    private int capacity;

    private String location;
}