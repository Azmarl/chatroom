package com.chatroom.chatroombackend.dto;

import lombok.Data;

@Data
public class SearchResultDto {
    private Long id; // 用户ID或群聊ID
    private String name; // 用户昵称或群聊名称
    private String avatarUrl;
    private String type; // "user" 或 "conversation"
}