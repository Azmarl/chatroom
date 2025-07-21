package com.chatroom.chatroombackend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "chatroom_activity_logs")
public class ChatroomActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id", nullable = false)
    private Conversation chatroom;

    @Column(name = "log_month", length = 7, nullable = false)
    private String logMonth; // e.g., "2025-07"

    @Column(name = "message_count")
    private Integer messageCount = 0;

    @Column(name = "active_user_count")
    private Integer activeUserCount = 0;
}