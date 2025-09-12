package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.dto.RegisterRequest;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.entity.RememberMeToken;
import com.chatroom.chatroombackend.enums.AccountStatus;
import com.chatroom.chatroombackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RememberMeTokenService rememberMeTokenService;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, RememberMeTokenService rememberMeTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rememberMeTokenService = rememberMeTokenService;
    }

    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional
    public User createAccount(RegisterRequest request) {
        // 1. 验证输入
        if (request.getUsername() == null || request.getUsername().isEmpty() ||
                request.getEmail() == null || request.getEmail().isEmpty() ||
                request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("用户名、邮箱和密码不能为空");
        }

        // 2. 检查用户名和邮箱是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("用户名已被占用");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("邮箱已被注册");
        }

        // 3. 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        // 关键：对密码进行加密存储
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setAccountStatus(AccountStatus.active); // 默认状态为激活

        // 4. 保存到数据库
        return userRepository.save(user);
    }

    public User login(String usernameOrEmail, String password) {
        // 1. 验证输入
        if (usernameOrEmail == null || usernameOrEmail.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("用户名/邮箱和密码不能为空");
        }

        // 2. 查找用户
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 3. 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        // 4. 检查账户状态
        if (user.getAccountStatus() != AccountStatus.active) {
            throw new IllegalStateException("账户状态异常，无法登录: " + user.getAccountStatus());
        }

        // 登录成功，返回用户实体
        return user;
    }

    /**
     * 新增：通过 "记住我" 的令牌进行登录。
     * @return 返回用户实体，如果令牌无效则抛出异常。
     */
    @Transactional
    public User loginWithToken(String tokenValue) {
        return rememberMeTokenService.findByTokenValue(tokenValue)
                .map(RememberMeToken::getUser)
                .orElseThrow(() -> new IllegalArgumentException("自动登录凭证无效或已过期"));
    }

    /**
     * 新增：用户登出时，清除其持久化令牌。
     */
    @Transactional
    public void logout(User user) {
        rememberMeTokenService.clearUserTokens(user);
    }
}