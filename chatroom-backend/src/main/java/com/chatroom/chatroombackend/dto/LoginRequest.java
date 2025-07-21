package com.chatroom.chatroombackend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username; // 用户名或邮箱
    private String password;
    private boolean rememberMe = false;
}