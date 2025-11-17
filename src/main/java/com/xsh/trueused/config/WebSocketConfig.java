package com.xsh.trueused.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 客户端发送消息的目标前缀
        registry.setApplicationDestinationPrefixes("/app");
        // 启用一个简单的内存消息代理，并为点对点消息（/queue）和广播消息（/topic）配置目标前缀
        registry.enableSimpleBroker("/topic", "/queue");
        // 为特定用户发送消息的目标前缀
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册一个 STOMP 端点，客户端将使用它来连接到 WebSocket 服务器
        // withSockJS() 是为了在浏览器不支持 WebSocket 时提供备用选项
        registry.addEndpoint("/ws").withSockJS();
    }
}