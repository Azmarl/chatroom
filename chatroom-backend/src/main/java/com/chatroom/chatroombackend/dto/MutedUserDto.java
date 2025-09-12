package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.entity.ConversationParticipant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MutedUserDto {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private LocalDateTime mutedUntil; // (核心修改) 返回截止时间点

    public static MutedUserDto fromEntity(ConversationParticipant participant) {
        return new MutedUserDto(
                participant.getUser().getId(),
                participant.getUser().getNickname(),
                participant.getUser().getAvatarUrl(),
                participant.getMutedUntil()
        );
    }
}
