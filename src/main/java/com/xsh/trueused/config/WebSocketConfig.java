package com.xsh.trueused.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.xsh.trueused.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

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
        registry.addEndpoint("/api/ws")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "http://192.168.*.*:*") // 明确允许的域
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    accessor = StompHeaderAccessor.wrap(message);
                }

                // 🛑 调试点 1: 看看所有流经的消息是什么命令
                log.info("【WS调试】拦截到消息，命令: {}", accessor.getCommand());
                log.info("【WS调试】当前用户: {}", accessor.getUser());

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    log.info("【WS调试】检测到 CONNECT 连接请求！准备提取 Token...");

                    // 🛑 调试点 2: 打印所有头部信息，看看 Token 到底藏在哪
                    log.info("【WS调试】Native Headers: {}", accessor.toNativeHeaderMap());

                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    log.info("【WS调试】提取到的 Authorization 头: {}", authHeader);

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        // 🛑 调试点 3: Token 是否有效？
                        boolean isValid = jwtTokenProvider.validateToken(token);
                        log.info("【WS调试】Token 校验结果: {}", isValid);

                        if (isValid) {
                            String username = jwtTokenProvider.getUsernameFromToken(token);
                            log.info("【WS调试】解析出的用户名: {}", username);

                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                            accessor.setUser(authentication);
                            log.info("【WS调试】✅ 成功设置用户认证信息: {}", username);
                        } else {
                            log.error("【WS调试】❌ Token 无效！");
                        }
                    } else {
                        log.warn("【WS调试】⚠️ Authorization 头为空或格式不对 (必须以 'Bearer ' 开头)");
                    }
                }
                return MessageBuilder.createMessage(message.getPayload(),
                        new MessageHeaders(accessor.getMessageHeaders()));
            }
        });
    }
}
