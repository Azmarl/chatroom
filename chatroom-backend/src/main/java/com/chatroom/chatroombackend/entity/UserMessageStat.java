package com.chatroom.chatroombackend.entity;

import com.chatroom.chatroombackend.entity.UserMessageStatId;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_message_stats")
public class UserMessageStat {

    @EmbeddedId
    private UserMessageStatId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "message_count")
    private Integer messageCount = 0;

    @Column(name = "word_count")
    private Integer wordCount = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}