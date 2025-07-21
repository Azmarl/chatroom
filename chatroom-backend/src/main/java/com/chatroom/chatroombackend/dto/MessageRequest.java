package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.enums.MessageType;
import lombok.Data;

@Data
public class MessageRequest {
    private String content;
    private MessageType type = MessageType.text; // 默认是文本消息
    private Long replyToMessageId; // For replying
}