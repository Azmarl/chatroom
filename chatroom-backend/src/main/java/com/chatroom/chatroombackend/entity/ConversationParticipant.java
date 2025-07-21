package com.chatroom.chatroombackend.entity;

import com.chatroom.chatroombackend.entity.ConversationParticipantId;
import com.chatroom.chatroombackend.enums.ParticipantRole;
import com.chatroom.chatroombackend.enums.ParticipantStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "conversation_participants")
public class ConversationParticipant {

    @EmbeddedId
    private ConversationParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversationId")
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "deleted_history_at")
    private LocalDateTime deletedHistoryAt;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('member', 'admin', 'owner')")
    private ParticipantRole role;

    @Column(name = "is_muted")
    private Boolean isMuted = false;  // for when an admin silences a user

    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;

    @Column(name = "is_pinned")
    private boolean isPinned = false; // For 'highlightConversation'

    @Column(name = "are_notifications_muted")
    private boolean areNotificationsMuted = false; // For 'muteConversation'

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "unread_count")
    private Integer unreadCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ParticipantStatus status;
}