package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class JwtAuthenticationResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private LoginResponse.UserInfo userInfo;

    public JwtAuthenticationResponse(String accessToken, LoginResponse.UserInfo userInfo) {
        this.accessToken = accessToken;
        this.userInfo = userInfo;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String nickname;
        private String avatarUrl;

        /**
         * 一个静态工厂方法，用于从 User 实体安全地创建 UserInfo DTO。
         * 这可以确保不会意外泄露密码等敏感信息。
         * @param user User 实体对象
         * @return UserInfo DTO 对象
         */
        public static LoginResponse.UserInfo fromUser(User user) {
            if (user == null) {
                return null;
            }
            return new LoginResponse.UserInfo(
                    user.getId(),
                    user.getUsername(),
                    user.getNickname(),
                    user.getAvatarUrl()
            );
        }
    }
}