package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.RememberMeToken;
import com.chatroom.chatroombackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface RememberMeTokenRepository extends JpaRepository<RememberMeToken, Long> {

    Optional<RememberMeToken> findByTokenValue(String tokenValue);

    @Transactional
    void deleteByUser(User user);
}