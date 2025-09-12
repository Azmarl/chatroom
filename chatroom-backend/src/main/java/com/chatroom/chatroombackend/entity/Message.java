package com.chatroom.chatroombackend.entity;

import com.chatroom.chatroombackend.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Lob
    private String content;

    @Column(name = "media_url", length = 255)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", columnDefinition = "ENUM('text', 'image', 'emoji', 'file', 'system')")
    private MessageType messageType;

    @Column(name = "is_deleted", columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message repliedToMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_recalled", columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isRecalled = false;
}