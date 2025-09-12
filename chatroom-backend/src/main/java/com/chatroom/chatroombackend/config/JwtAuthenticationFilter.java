package com.chatroom.chatroombackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider; // 使用 JwtTokenProvider
    private final UserDetailsService userDetailsService; // 保持不变

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // "Bearer "是7个字符

        // 使用 Access Token 验证和提取用户名
        if (jwtTokenProvider.validateAccessToken(jwt)) { // 使用 validateAccessToken
            username = jwtTokenProvider.getUsernameFromAccessToken(jwt); // 使用 getUsernameFromAccessToken

            // 如果用户名存在且SecurityContext中没有认证信息
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                // 此时 token 已经通过 validateAccessToken 验证，无需再次验证 isTokenValid
                // 创建一个已认证的Authentication对象
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // JWT认证不需要凭证
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                // 更新SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } else {
            // 如果 Access Token 无效或过期，这里不做处理，让响应拦截器去处理 401
            logger.warn("Access Token is invalid or expired for request to: {}");
        }
        filterChain.doFilter(request, response);
    }
}