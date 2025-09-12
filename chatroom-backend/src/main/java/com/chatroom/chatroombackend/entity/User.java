package com.chatroom.chatroombackend.entity;

import com.chatroom.chatroombackend.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true, nullable = false)
    private String username;

    @Column(length = 100, unique = true, nullable = false)
    private String email;

    @Column(length = 255, nullable = false)
    private String password;

    @Column(length = 50)
    private String nickname;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", columnDefinition = "ENUM('active', 'suspended', 'banned')")
    private AccountStatus accountStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    // Many-to-Many relationship for user interests
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_interest_tags",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> interestedTags;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 简单起见，我们给所有用户一个 "ROLE_USER" 的角色
        // 在复杂的系统中，这里应该从用户的角色列表动态生成
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * getPassword() 方法已经由 @Data (Lombok) 自动生成。
     * @return 返回加密后的密码
     */
    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * getUsername() 方法已经由 @Data (Lombok) 自动生成。
     * @return 返回用于登录的用户名
     */
    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * 账户是否未过期
     */
    @Override
    public boolean isAccountNonExpired() {
        return true; // 或者根据你的业务逻辑判断
    }

    /**
     * 账户是否未被锁定
     */
    @Override
    public boolean isAccountNonLocked() {
        // 我们可以将会话状态与锁定状态关联
        return this.accountStatus != AccountStatus.suspended && this.accountStatus != AccountStatus.banned;
    }

    /**
     * 凭证（密码）是否未过期
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 或者根据你的业务逻辑判断
    }

    /**
     * 账户是否启用
     */
    @Override
    public boolean isEnabled() {
        return this.accountStatus == AccountStatus.active;
    }
}
