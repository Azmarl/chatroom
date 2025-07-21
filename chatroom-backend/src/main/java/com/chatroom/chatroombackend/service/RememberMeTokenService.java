package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.entity.RememberMeToken;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.repository.RememberMeTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RememberMeTokenService {

    // 令牌有效期，例如 7 天
    private static final int EXPIRATION_DAYS = 7;

    private final RememberMeTokenRepository tokenRepository;

    @Autowired
    public RememberMeTokenService(RememberMeTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * 为指定用户创建一个新的 "记住我" 令牌。
     */
    @Transactional
    public RememberMeToken createToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(EXPIRATION_DAYS);

        RememberMeToken token = new RememberMeToken(tokenValue, user, expiryDate);
        return tokenRepository.save(token);
    }

    /**
     * 通过令牌值查找令牌，并检查是否过期。
     */
    public Optional<RememberMeToken> findByTokenValue(String tokenValue) {
        Optional<RememberMeToken> optionalToken = tokenRepository.findByTokenValue(tokenValue);
        if (optionalToken.isPresent()) {
            RememberMeToken token = optionalToken.get();
            if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
                // 令牌已过期，从数据库删除
                tokenRepository.delete(token);
                return Optional.empty();
            }
        }
        return optionalToken;
    }

    /**
     * 清除指定用户的所有令牌（例如，在用户登出或修改密码时调用）。
     */
    @Transactional
    public void clearUserTokens(User user) {
        tokenRepository.deleteByUser(user);
    }

    /**
     * 删除单个令牌。
     */
    @Transactional
    public void deleteToken(String tokenValue) {
        findByTokenValue(tokenValue).ifPresent(tokenRepository::delete);
    }
}