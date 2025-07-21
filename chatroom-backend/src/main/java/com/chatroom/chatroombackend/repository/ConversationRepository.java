package com.chatroom.chatroombackend.repository;


import com.chatroom.chatroombackend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // 统计用户创建的群聊数量
    long countByOwnerId(Long ownerId);

    // 获取用户参与的所有聊天
    @Query("SELECT c FROM Conversation c JOIN ConversationParticipant p WHERE p.user.id = :userId AND c.id = p.conversation.id")
    List<Conversation> findAllByParticipantUserId(Long userId);

    // 搜索用户参与的、名称匹配的群聊
    @Query("SELECT c FROM Conversation c JOIN ConversationParticipant p WHERE p.user.id = :userId AND c.id = p.conversation.id AND c.type = 'GROUP' AND c.name LIKE %:nameQuery%")
    List<Conversation> findGroupConversationsByName(Long userId, String nameQuery);

    @Query("SELECT c FROM Conversation c JOIN ConversationParticipant p WHERE p.user.id = :userId AND c.id = p.conversation.id AND c.type = 'GROUP' AND p.id IS NULL")
    List<Conversation> findAllPublicGroupsNotIn(Long userId);
}