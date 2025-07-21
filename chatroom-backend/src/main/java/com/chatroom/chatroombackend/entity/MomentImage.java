package com.chatroom.chatroombackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "moment_images")
public class MomentImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moment_id", nullable = false)
    @JsonIgnore // 避免序列化时产生循环引用
    private Moment moment;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "order_in_moment")
    private Integer orderInMoment = 0;
}
