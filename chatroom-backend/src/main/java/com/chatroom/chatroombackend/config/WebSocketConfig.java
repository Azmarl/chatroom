package com.chatroom.chatroombackend.config; // 确保包名正确

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 启用 WebSocket 消息代理
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 1. 设置消息代理的前缀，所有发往这些前缀的都会被路由到消息代理（broker）
        //    客户端将订阅这些前缀的目的地
        registry.enableSimpleBroker("/topic", "/queue");

        // 2. 设置应用的前缀，所有发往这些前缀的都会被路由到 @MessageMapping 注解的方法
        //    客户端发送消息时，目的地会带上这个前缀
        registry.setApplicationDestinationPrefixes("/app");

        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. 注册一个 STOMP 端点，客户端将连接到这个端点
        //    /ws 是我们选择的端点路径
        registry.addEndpoint("/ws")
                // 2. 允许来自指定源的跨域请求，以便前端可以连接
                .setAllowedOrigins("http://localhost:5173")
                // 3. 启用 SockJS 作为备用选项，以防浏览器不支持原生 WebSocket
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor); // 2. 将拦截器添加到处理链中
    }
}
