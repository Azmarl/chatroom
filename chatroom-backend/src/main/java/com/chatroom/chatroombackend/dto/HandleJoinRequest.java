package com.chatroom.chatroombackend.dto;

import lombok.Data;

@Data
public class HandleJoinRequest {
    private Long requesterId;
    private String action; // "ACCEPT" 或 "REJECT"
}