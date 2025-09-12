package com.chatroom.chatroombackend.dto;

import lombok.Data;

@Data
public class UserSearchResultDto {

    // 用户的唯一ID，前端在发送好友请求时需要用到
    private Long id;

    // 用户名
    private String username;

    // 昵称
    private String nickname;

    // 头像URL
    private String avatarUrl;
}