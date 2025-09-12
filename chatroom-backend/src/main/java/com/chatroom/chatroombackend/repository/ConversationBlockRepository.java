package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.ConversationBlock;
import com.chatroom.chatroombackend.entity.ConversationParticipant;
import com.chatroom.chatroombackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConversationBlockRepository extends JpaRepository<ConversationBlock, Long> {
  boolean existsByConversationIdAndBlockedUserId(Long conversationId, Long blockedUserId);

  long countByBlockedUser(User user);

  List<ConversationBlock> findAllByBlockedUser(User user);

  @Query(
      "SELECT b FROM ConversationBlock b WHERE b.conversation.id = :conversationId AND b.blockedUser.id = :blockedUserId")
  ConversationBlock findByConversationIdAndUserId(Long conversationId, Long blockedUserId);
}
