package com.chatroom.chatroombackend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ForwardMessageRequest {
    private Long originalMessageId;
    private List<Long> targetConversationIds;
    private String attachedMessage; // 转发时附带的留言
}