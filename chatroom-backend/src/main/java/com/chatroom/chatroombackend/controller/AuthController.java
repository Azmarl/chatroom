package com.chatroom.chatroombackend.controller;

import com.chatroom.chatroombackend.config.JwtTokenProvider;
import com.chatroom.chatroombackend.dto.JwtAuthenticationResponse;
import com.chatroom.chatroombackend.dto.LoginRequest;
import com.chatroom.chatroombackend.dto.LoginResponse;
import com.chatroom.chatroombackend.dto.RegisterRequest;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.repository.UserRepository;
import com.chatroom.chatroombackend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    /**
     * (核心新增) 获取当前已认证用户的信息。
     * 这个端点可以用来在应用启动时验证 accessToken 的有效性。
     * @param userDetails 由Spring Security注入的当前用户信息
     * @return 包含用户信息的ResponseEntity
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            // 这通常不会发生，因为SecurityConfig会拦截未认证的请求
            return ResponseEntity.status(401).body("Not authenticated");
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl()
        );

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/register")
    public ResponseEntity<?> createAccount(@RequestBody RegisterRequest registerRequest) {
        try {
            if (userRepository.existsByUsername(registerRequest.getUsername())) {
                return ResponseEntity.badRequest().body("用户名已被占用！");
            }
            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.badRequest().body("邮箱已被占用！");
            }

            User newUser = authService.createAccount(registerRequest);
            return ResponseEntity.ok("账户创建成功！用户ID: " + newUser.getId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("服务器发生未知错误：" + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 生成 Access Token
            String accessToken = tokenProvider.generateAccessToken(authentication);
            // 生成 Refresh Token
            String refreshToken = tokenProvider.generateRefreshToken(authentication); // 新增方法

            // (核心修改) 将 Refresh Token Cookie 的路径设置为根路径 "/"
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/") // <--- 修改为根路径
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Strict")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // 获取用户详情并构建 UserResponse
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
            LoginResponse.UserInfo userInfo =
          new LoginResponse.UserInfo(
              user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl());

            return ResponseEntity.ok(new JwtAuthenticationResponse(accessToken, userInfo));

        } catch (Exception e) {
            return ResponseEntity.status(401).body("登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 从 Cookie 中获取 Refresh Token
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null || !tokenProvider.validateRefreshToken(refreshToken)) { // 新增验证 Refresh Token 方法
            return ResponseEntity.status(401).body("Invalid Refresh Token");
        }

        String username = tokenProvider.getUsernameFromRefreshToken(refreshToken); // 新增从 Refresh Token 获取用户名方法
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty() || !userOptional.get().isEnabled() || !userOptional.get().isAccountNonLocked()) {
            return ResponseEntity.status(401).body("User associated with Refresh Token is invalid or disabled.");
        }

        // 重新生成 Access Token
        // 注意：这里我们不通过 AuthenticationManager，因为 Refresh Token 已经验证了用户身份
        // 我们直接根据 Refresh Token 中的用户信息生成新的 Access Token
        User user = userOptional.get();
        // 创建一个临时的 Authentication 对象来生成新的 Access Token
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());

        String newAccessToken = tokenProvider.generateAccessToken(authentication);

        // (可选) 令牌轮换：生成新的 Refresh Token 并替换旧的 Cookie
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);
        ResponseCookie newRefreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, newRefreshCookie.toString());

        LoginResponse.UserInfo userInfo =
                new LoginResponse.UserInfo(
                        user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl());

        return ResponseEntity.ok(new JwtAuthenticationResponse(newAccessToken, userInfo));
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // 清除 Refresh Token Cookie
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/") // 路径要与设置时一致
                .maxAge(0) // 立即过期
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("已成功登出");
    }
}