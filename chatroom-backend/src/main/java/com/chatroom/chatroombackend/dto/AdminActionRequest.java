package com.chatroom.chatroombackend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminActionRequest {
    private Long targetUserId;
    private LocalDateTime silenceUntil; // For silencing
    private String reason; // For blocking
    private String action;
}