package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    /**
     * 短生命周期的访问令牌 (Access Token)。
     * 前端将使用此令牌来访问所有受保护的API。
     */
    private String accessToken;

    /**
     * 用于在前端直接更新UI的用户基本信息。
     */
    private UserInfo userInfo;

    /**
     * 嵌套的静态DTO，只包含公开、安全的用户信息。
     */
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
        public static UserInfo fromUser(User user) {
            if (user == null) {
                return null;
            }
            return new UserInfo(
                    user.getId(),
                    user.getUsername(),
                    user.getNickname(),
                    user.getAvatarUrl()
            );
        }
    }
}