package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.enums.ConversationType;
import lombok.Data;
import java.time.LocalDateTime;

// 用于在首页列表展示的聊天概要
@Data
public class ConversationSummaryDto {
    private Long conversationId;
    private String uuid;
    private ConversationType type;
    private String name; // 私聊时是对方昵称，群聊时是群名称
    private String avatarUrl; // 对方头像或群头像

    // 最后一条消息的预览
    private String lastMessageContent;
    private LocalDateTime lastMessageTimestamp;

    // 当前用户的未读消息数
    private int unreadCount;
    private boolean Pinned;
    private boolean NotificationsMuted;
}