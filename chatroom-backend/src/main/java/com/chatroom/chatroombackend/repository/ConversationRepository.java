package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.dto.SearchResultDto;
import com.chatroom.chatroombackend.entity.Conversation;
import com.chatroom.chatroombackend.entity.ConversationBlock;
import com.chatroom.chatroombackend.entity.ConversationParticipant;
import com.chatroom.chatroombackend.enums.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

  // 统计用户创建的群聊数量
  long countByOwnerId(Long ownerId);

  // 获取用户参与的所有聊天
  @Query(
      "SELECT c FROM Conversation c JOIN ConversationParticipant p ON c.id = p.conversation.id WHERE p.user.id = :userId")
  List<Conversation> findAllByParticipantUserId(Long userId);

  // 搜索用户参与的、名称匹配的群聊
  @Query(
      "SELECT c FROM Conversation c JOIN ConversationParticipant p ON c.id = p.conversation.id WHERE p.user.id = :userId AND c.type = 'GROUP' AND c.name LIKE %:nameQuery%")
  List<Conversation> findGroupConversationsByName(
      @Param("userId") Long userId, @Param("nameQuery") String nameQuery);

  @Query(
      "SELECT c FROM Conversation c JOIN ConversationParticipant p ON c.id = p.conversation.id WHERE p.user.id = :userId AND c.type = 'GROUP' AND p.id IS NULL")
  List<Conversation> findAllPublicGroupsNotIn(Long userId);

  // 用于通过公开ID查找群聊
  Optional<Conversation> findByUuid(String uuid);

  @Query(
      "SELECT c FROM Conversation c JOIN ConversationParticipant p ON c.id = p.conversation.id WHERE p.user.id = :userId AND c.type = :type")
  List<Conversation> findConversationsByParticipantAndType(
      @Param("userId") Long userId, @Param("type") ConversationType type);

  @Query("SELECT p FROM ConversationParticipant p WHERE p.conversation.id = :conversationId")
  List<ConversationParticipant> findAllUserByConversationId(
      @Param("conversationId") Long conversationId);

  @Query(
      "SELECT p FROM ConversationParticipant p WHERE p.conversation.uuid = :uuid and p.user.username = :username")
  ConversationParticipant findUserByConversationIdAndUsername(
      @Param("uuid") String uuid, @Param("username") String username);

  @Query("SELECT b FROM ConversationBlock b WHERE b.conversation.id = :conversationId")
  List<ConversationBlock> getBlackListByConversationId(
      @Param("conversationId") Long conversationId);

  @Query(
      "SELECT c FROM Conversation c WHERE c.type = 'PRIVATE' "
          + "AND EXISTS (SELECT 1 FROM ConversationParticipant cp1 WHERE cp1.conversation = c AND cp1.user.id = :userId1) "
          + "AND EXISTS (SELECT 1 FROM ConversationParticipant cp2 WHERE cp2.conversation = c AND cp2.user.id = :userId2)")
  Optional<Conversation> findPrivateConversationBetweenUsers(
      @Param("userId1") Long userId1, @Param("userId2") Long userId2);

  @Query(
      "SELECT c.id FROM Conversation c WHERE c.type = 'PRIVATE' "
          + "AND EXISTS (SELECT 1 FROM ConversationParticipant cp1 WHERE cp1.conversation = c AND cp1.user.id = :id) "
          + "AND EXISTS (SELECT 1 FROM ConversationParticipant cp2 WHERE cp2.conversation = c AND cp2.user.id = :targetId)")
  ResponseEntity<Long> getConversationIdByUserAndTargetId(
      @Param("id") Long id, @Param("targetId") Long targetId);
}
