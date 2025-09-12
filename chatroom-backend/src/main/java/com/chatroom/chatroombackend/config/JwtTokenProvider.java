package com.chatroom.chatroombackend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    // 从 application.properties 读取 Access Token 的密钥和过期时间
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

    // 从 application.properties 读取 Refresh Token 的密钥和过期时间
    @Value("${app.jwtRefreshSecret}")
    private String jwtRefreshSecret;

    @Value("${app.jwtRefreshExpirationMs}")
    private int jwtRefreshExpirationMs;

    private Key accessKey;  // Access Token 的签名密钥
    private Key refreshKey; // Refresh Token 的签名密钥

    // 在 Bean 初始化后执行，用于生成密钥
    @PostConstruct
    public void init() {
        // 使用 HS512 算法生成一个安全的密钥
        // 密钥长度至少应为 256 位 (32 字节)，否则会报错
        this.accessKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.refreshKey = Keys.hmacShaKeyFor(jwtRefreshSecret.getBytes());
    }

    /**
     * 生成 Access Token
     * @param authentication 认证对象，包含用户详情
     * @return 生成的 JWT 字符串
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername()) // JWT 的主题，通常是用户名
                .setIssuedAt(new Date())                 // 签发时间
                .setExpiration(expiryDate)               // 过期时间
                .signWith(accessKey, SignatureAlgorithm.HS512) // 使用 Access Token 密钥和 HS512 算法签名
                .compact(); // 压缩成 JWT 字符串
    }

    /**
     * 生成 Refresh Token
     * @param authentication 认证对象，包含用户详情
     * @return 生成的 Refresh Token 字符串
     */
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername()) // JWT 的主题，通常是用户名
                .setIssuedAt(new Date())                 // 签发时间
                .setExpiration(expiryDate)               // 过期时间
                .signWith(refreshKey, SignatureAlgorithm.HS512) // 使用 Refresh Token 密钥和 HS512 算法签名
                .compact(); // 压缩成 JWT 字符串
    }

    /**
     * 从 Access Token 中提取所有 Claims (负载)
     * @param token Access Token 字符串
     * @return Claims 对象
     */
    private Claims extractAccessClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey) // 使用 Access Token 密钥
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 Refresh Token 中提取所有 Claims (负载)
     * @param token Refresh Token 字符串
     * @return Claims 对象
     */
    private Claims extractRefreshClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(refreshKey) // 使用 Refresh Token 密钥
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 Access Token 中获取用户名
     * @param token Access Token 字符串
     * @return 用户名
     */
    public String getUsernameFromAccessToken(String token) {
        return extractAccessClaims(token).getSubject();
    }

    /**
     * 从 Refresh Token 中获取用户名
     * @param token Refresh Token 字符串
     * @return 用户名
     */
    public String getUsernameFromRefreshToken(String token) {
        return extractRefreshClaims(token).getSubject();
    }

    /**
     * 验证 Access Token 是否有效
     * @param authToken Access Token 字符串
     * @return 如果有效返回 true，否则返回 false
     */
    public boolean validateAccessToken(String authToken) {
        try {
            extractAccessClaims(authToken); // 尝试解析 Claims，如果失败则抛出异常
            return true;
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            logger.error("Invalid Access Token signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid Access Token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired Access Token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported Access Token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("Access Token claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * 验证 Refresh Token 是否有效
     * @param authToken Refresh Token 字符串
     * @return 如果有效返回 true，否则返回 false
     */
    public boolean validateRefreshToken(String authToken) {
        try {
            extractRefreshClaims(authToken); // 尝试解析 Claims，如果失败则抛出异常
            return true;
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            logger.error("Invalid Refresh Token signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid Refresh Token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired Refresh Token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported Refresh Token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("Refresh Token claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * (核心新增) 验证一个 Access Token 是否对某个特定用户有效。
     * @param token JWT字符串
     * @param userDetails 从数据库加载的用户详情
     * @return 如果token有效且属于该用户，则返回true
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsernameFromAccessToken(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // --- (重构) 将重复的解析逻辑提取到一个通用方法中 ---
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}