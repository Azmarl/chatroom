package com.chatroom.chatroombackend.entity;

import com.chatroom.chatroombackend.enums.ConversationType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('private', 'group')")
    private ConversationType type;

    @Column(length = 100)
    private String name;

    @Lob
    private String description;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "member_limit")
    private Integer memberLimit;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    // Geolocation fields,通过浏览器的Geolocation API获取用户当前大致的经纬度，再通过例如Google Maps API反向解析出城市
    private Double latitude;
    private Double longitude;
    @Column(length = 100)
    private String city;

    // Many-to-Many relationship with Tag
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "conversation_tags",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags;

    @Column(name = "is_public")
    private boolean isPublic = true;
}