package com.chatroom.chatroombackend.entity;

import com.chatroom.chatroombackend.enums.ParticipantRole;
import com.chatroom.chatroombackend.enums.ParticipantStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "conversation_participants")
public class ConversationParticipant {

    /**
     * 使用 @EmbeddedId 注解来引入复合主键。
     */
    @EmbeddedId
    private ConversationParticipantId id = new ConversationParticipantId();

    /**
     * 使用 @MapsId 将实体中的 'conversation' 字段映射到
     * EmbeddedId 中的 'conversationId' 字段。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversationId")
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    /**
     * 使用 @MapsId 将实体中的 'user' 字段映射到
     * EmbeddedId 中的 'userId' 字段。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * (核心) 提供一个方便的构造函数，用于在服务层轻松创建实例。
     * 它会正确地设置所有关联关系和复合主键。
     * @param conversation 关联的会话
     * @param user         关联的用户
     */
    public ConversationParticipant(Conversation conversation, User user) {
        // 设置关系
        this.conversation = conversation;
        this.user = user;

        // 设置复合主键的值
        this.id.setConversationId(conversation.getId());
        this.id.setUserId(user.getId());

        // 设置一些安全的默认值
        this.joinedAt = LocalDateTime.now();
        this.role = ParticipantRole.member;
        this.status = ParticipantStatus.APPROVED;
        this.isMuted = false;
        this.isPinned = false;
        this.areNotificationsMuted = false;
        this.unreadCount = 0;
    }

    // --- 以下是您提供的所有字段 ---

    @Column(name = "deleted_history_at")
    private LocalDateTime deletedHistoryAt;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('member', 'admin', 'owner', 'waiting')")
    private ParticipantRole role;

    @Column(name = "is_muted")
    private Boolean isMuted;

    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;

    @Column(name = "is_pinned")
    private boolean isPinned;

    @Column(name = "are_notifications_muted")
    private boolean areNotificationsMuted;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "unread_count")
    private Integer unreadCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ParticipantStatus status;
}
