package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.entity.ConversationParticipant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupJoinRequestDto {
    private Long requesterId;
    private String nickname;
    private String avatarUrl;

    /**
     * 从实体对象创建DTO的静态工厂方法。
     */
    public static GroupJoinRequestDto fromEntity(ConversationParticipant participant) {
        return new GroupJoinRequestDto(
                participant.getUser().getId(),
                participant.getUser().getNickname(),
                participant.getUser().getAvatarUrl()
        );
    }
}