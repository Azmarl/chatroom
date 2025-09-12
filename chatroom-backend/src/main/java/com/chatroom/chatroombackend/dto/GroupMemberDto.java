package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.enums.ParticipantRole;
import lombok.Data;

@Data
public class GroupMemberDto {
    private Long id;

    // 用户名
    private String username;

    // 昵称
    private String nickname;

    // 头像URL
    private String avatarUrl;

    private ParticipantRole role;
}
