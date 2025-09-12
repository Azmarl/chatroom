package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.*;
import com.chatroom.chatroombackend.enums.ParticipantRole;
import com.chatroom.chatroombackend.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface ConversationParticipantRepository
    extends JpaRepository<ConversationParticipant, ConversationParticipantId> {
  Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

  List<ConversationParticipant> findAllByConversation(Conversation conversation);

  long countByUser(User user);

  long countByConversation(Conversation conversation);

  List<ConversationParticipant> findAllByUser(User user);

  /**
   * Finds all pending join requests for groups where the specified user is an admin or owner. This
   * is a more complex query that first finds the groups the user manages and then finds pending
   * requests within those groups.
   *
   * @param adminOrOwner The user who is an admin or owner.
   * @param status The status of the participant to find (should be PENDING).
   * @return A list of pending ConversationParticipant entities.
   */
  @Query(
      "SELECT p_pending FROM ConversationParticipant p_pending "
          + "WHERE p_pending.status = :status "
          + "AND p_pending.conversation.id IN "
          + "(SELECT p_admin.conversation.id FROM ConversationParticipant p_admin "
          + "WHERE p_admin.user = :adminOrOwner AND p_admin.role IN :roles)")
  List<ConversationParticipant> findPendingJoinRequestsForAdmin(
      @Param("adminOrOwner") User adminOrOwner,
      @Param("status") ParticipantStatus status,
      @Param("roles") List<ParticipantRole> roles // <-- 传入 List<ParticipantRole>
      );

  List<ConversationParticipant> findByConversationAndStatus(
      Conversation conversation, ParticipantStatus status);

  @Query(
      "SELECT p FROM ConversationParticipant p WHERE p.conversation.id = :conversationId AND p.isMuted = true")
  List<ConversationParticipant> findByConversationIdAndIsMutedTrue(Long conversationId);

  List<ConversationParticipant> findByConversationIdAndUserIdNot(Long conversationId, Long id);

  // 添加按会话ID删除参与者的方法
  @Modifying
  @Query("DELETE FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId")
  void deleteByConversationId(@Param("conversationId") Long conversationId);

  @Query("SELECT cp.user FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId AND cp.user.id != :id")
  Optional<User> findPartnerInPrivateConversation(Long conversationId, Long id);
}
