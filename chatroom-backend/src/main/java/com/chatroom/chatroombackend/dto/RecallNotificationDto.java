package com.chatroom.chatroombackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecallNotificationDto {
    private Long conversationId;
    private Long messageId;
}