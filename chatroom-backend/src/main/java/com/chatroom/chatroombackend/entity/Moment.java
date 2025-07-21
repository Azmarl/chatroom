package com.chatroom.chatroombackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "moments")
public class Moment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_id", nullable = false)
    private User poster;

    @Lob // 对于 TEXT 类型，使用 @Lob
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "likes_count")
    private Integer likesCount = 0;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "comments_count")
    private Integer commentsCount = 0;

    // 一对多关系，一个动态有多张图片
    @OneToMany(mappedBy = "moment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orderInMoment ASC")
    private List<MomentImage> images;
}
