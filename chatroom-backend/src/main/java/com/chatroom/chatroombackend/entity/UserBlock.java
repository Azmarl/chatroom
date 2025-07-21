package com.chatroom.chatroombackend.entity;

import com.chatroom.chatroombackend.entity.UserBlockId;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "black_list")
public class UserBlock {

    @EmbeddedId
    private UserBlockId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("blockerId") // 映射到 UserBlockId 中的 blockerId 字段
    @JoinColumn(name = "blocker_id")
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("blockedId") // 映射到 UserBlockId 中的 blockedId 字段
    @JoinColumn(name = "blocked_id")
    private User blocked;

    @CreationTimestamp
    @Column(name = "blocked_at", nullable = false, updatable = false)
    private LocalDateTime blockedAt;
}
