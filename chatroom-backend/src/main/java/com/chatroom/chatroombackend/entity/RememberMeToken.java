package com.chatroom.chatroombackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "remember_me_tokens")
public class RememberMeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 存储在 cookie 中的令牌值，必须长且随机
    @Column(nullable = false, unique = true, length = 255)
    private String tokenValue;

    // 关联的用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 令牌的过期时间
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public RememberMeToken(String tokenValue, User user, LocalDateTime expiryDate) {
        this.tokenValue = tokenValue;
        this.user = user;
        this.expiryDate = expiryDate;
    }
}