package com.chatroom.chatroombackend.config; // 建议放在 config 包下

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 1. 判断是否为连接请求
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 2. 从请求头中获取 "Authorization"
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            log.info("Authorization header: {}", authHeader);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                String username = jwtService.getUsernameFromAccessToken(jwt);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        // 3. 如果token有效，则创建一个认证对象，并设置到 Spring Security 的上下文中
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        // 注意：这里我们将认证信息设置到 STOMP 的头中，以便后续的处理器可以获取
                        accessor.setUser(authToken);
                        log.info("Authenticated user {} for WebSocket session.", username);
                    }
                }
            }
        }
        return message;
    }
}