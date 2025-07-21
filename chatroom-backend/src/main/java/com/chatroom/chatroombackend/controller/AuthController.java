package com.chatroom.chatroombackend.controller;

import com.chatroom.chatroombackend.dto.LoginRequest;
import com.chatroom.chatroombackend.dto.LoginResponse;
import com.chatroom.chatroombackend.dto.RegisterRequest;
import com.chatroom.chatroombackend.entity.RememberMeToken;
import com.chatroom.chatroombackend.entity.User;
import com.chatroom.chatroombackend.service.AuthService;
import com.chatroom.chatroombackend.service.RememberMeTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/") // 映射到根目录
public class AuthController {

    private final AuthService authService;
    private final RememberMeTokenService tokenService;

    @Autowired
    public AuthController(AuthService authService, RememberMeTokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> createAccount(@RequestBody RegisterRequest registerRequest) {
        try {
            User newUser = authService.createAccount(registerRequest);
            // 注册成功，可以返回成功信息，或者直接返回用户信息（不含密码）
            return ResponseEntity.ok("账户创建成功！用户ID: " + newUser.getId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 处理业务逻辑异常（如用户已存在）
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // 处理其他未知异常
            return ResponseEntity.internalServerError().body("服务器发生未知错误");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody(required = false) LoginRequest loginRequest,
            @CookieValue(name = "remember-me", required = false) String rememberMeCookie
    ) {
        try {
            User user;
            String newTokenValue = null;

            // 优先使用 Cookie 自动登录
            if (rememberMeCookie != null && !rememberMeCookie.isEmpty()) {
                user = authService.loginWithToken(rememberMeCookie);
                // 安全增强：令牌轮换（Token Rotation）
                // 删除旧令牌，创建一个新令牌
                tokenService.deleteToken(rememberMeCookie);
                RememberMeToken newRememberMeToken = tokenService.createToken(user);
                newTokenValue = newRememberMeToken.getTokenValue();
            }
            // 如果没有 Cookie，则使用用户名密码登录
            else if (loginRequest != null) {
                user = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
                // 如果用户选择了 "记住我"，则创建令牌
                if (loginRequest.isRememberMe()) {
                    RememberMeToken rememberMeToken = tokenService.createToken(user);
                    newTokenValue = rememberMeToken.getTokenValue();
                }
            }
            else {
                return ResponseEntity.badRequest().body("请输入登录信息或提供有效的自动登录凭证");
            }

            // --- 登录成功后的操作 ---

            // 构建响应体
            LoginResponse response = new LoginResponse();
            response.setMessage("登录成功");
            response.setUserInfo(LoginResponse.UserInfo.fromUser(user));

            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();

            // 如果生成了新令牌，就设置到 Cookie 中
            if (newTokenValue != null) {
                ResponseCookie cookie = ResponseCookie.from("remember-me", newTokenValue)
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(Duration.ofDays(7))
                        .sameSite("Strict")
                        .build();
                responseBuilder.header(HttpHeaders.SET_COOKIE, cookie.toString());
            }

            return responseBuilder.body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            // 如果登录失败，确保清除了可能无效的 "remember-me" cookie
            ResponseCookie deleteCookie = ResponseCookie.from("remember-me", "")
                    .path("/")
                    .maxAge(0) // 让 cookie 立即过期
                    .build();
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("登录时发生未知错误: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "remember-me", required = false) String rememberMeCookie
    ) {
        // 如果存在 "记住我" 的 cookie，就从数据库中删除对应的 token
        if (rememberMeCookie != null) {
            tokenService.deleteToken(rememberMeCookie);
        }

        // 创建一个让浏览器端 "remember-me" cookie 失效的响应
        ResponseCookie deleteCookie = ResponseCookie.from("remember-me", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // 立即过期
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body("已成功登出");
    }
}