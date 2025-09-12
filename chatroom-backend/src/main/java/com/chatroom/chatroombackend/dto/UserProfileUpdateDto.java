package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用于通过WebSocket广播用户资料更新的通知。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDto {
    private Long userId;
    private String newNickname;
    private String newAvatarUrl;

    public static UserProfileUpdateDto fromUser(User user) {
        return new UserProfileUpdateDto(
                user.getId(),
                user.getNickname(),
                user.getAvatarUrl()
        );
    }
}
