package com.chatroom.chatroombackend.dto;

import com.chatroom.chatroombackend.entity.Conversation;
import com.chatroom.chatroombackend.enums.ParticipantRole;
import lombok.Data;
import java.util.List;

@Data
public class GroupDetailsDto {
    private Long conversationId;
    private String uuid;
    private String name;
    private String avatarUrl;
    private String description;
    private int memberCount;
    private List<GroupMemberDto> members;
    private ParticipantRole currentUserRole;

    // 一个方便的静态工厂方法，用于从实体转换
    public static GroupDetailsDto fromEntity(Conversation conversation, List<GroupMemberDto> members, ParticipantRole currentUserRole) {
        GroupDetailsDto dto = new GroupDetailsDto();
        dto.setConversationId(conversation.getId());
        dto.setUuid(conversation.getUuid());
        dto.setName(conversation.getName());
        dto.setAvatarUrl(conversation.getAvatarUrl());
        dto.setDescription(conversation.getDescription());
        dto.setMemberCount(members.size());
        dto.setMembers(members);
        dto.setCurrentUserRole(currentUserRole);
        return dto;
    }
}