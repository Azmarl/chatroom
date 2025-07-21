package com.chatroom.chatroombackend.dto;

import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class CreateConversationRequest {
    private String name; // 群聊名称
    private String description; // 群聊描述
    private List<Long> initialMemberIds; // 初始化时邀请的好友ID列表
    private Set<String> tags;
}