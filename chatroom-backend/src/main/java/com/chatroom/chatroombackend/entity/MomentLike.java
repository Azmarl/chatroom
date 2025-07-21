package com.chatroom.chatroombackend.entity;

import com.chatroom.chatroombackend.entity.MomentLikeId;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "moment_likes")
public class MomentLike {

    @EmbeddedId
    private MomentLikeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("momentId")
    @JoinColumn(name = "moment_id")
    private Moment moment;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(name = "liked_at", nullable = false, updatable = false)
    private LocalDateTime likedAt;
}