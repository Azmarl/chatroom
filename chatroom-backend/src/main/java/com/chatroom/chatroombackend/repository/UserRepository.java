package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 检查用户名是否存在
    boolean existsByUsername(String username);

    // 检查邮箱是否存在
    boolean existsByEmail(String email);

    // 通过用户名或邮箱查找用户
    Optional<User> findByUsernameOrEmail(String username, String email);

    // 仅通过用户名查找
    Optional<User> findByUsername(String username);

    // 仅通过邮箱查找
    Optional<User> findByEmail(String email);
}