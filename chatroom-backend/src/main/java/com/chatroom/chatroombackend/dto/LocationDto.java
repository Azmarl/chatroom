package com.chatroom.chatroombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocationDto {
    private Double latitude;
    private Double longitude;
    private String city;
}