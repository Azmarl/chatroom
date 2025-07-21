package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    private String message;
    private UserInfo userInfo;

    // 内部类，只包含需要返回给前端的用户信息
    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String nickname;
//        private String avatarUrl;

        public static UserInfo fromUser(User user) {
            UserInfo dto = new UserInfo();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setNickname(user.getNickname());
//            dto.setAvatarUrl(user.getAvatarUrl());
            return dto;
        }
    }
}