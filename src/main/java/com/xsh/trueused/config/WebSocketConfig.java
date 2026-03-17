package com.xsh.trueused.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.xsh.trueused.security.jwt.JwtTokenProvider;
import com.xsh.trueused.security.service.TokenRevocationService;
import com.xsh.trueused.security.user.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenRevocationService tokenRevocationService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 客户端发送消息的目标前缀
        registry.setApplicationDestinationPrefixes("/app");

        // 启用一个简单的内存消息代理，并为点对点消息（/queue）和广播消息（/topic）配置目标前缀
        // 配置心跳: {发送间隔, 接收间隔} 单位毫秒。这里设为 10秒。
        // 这能确保如果客户端意外断开（如直接关闭浏览器），服务器能在 10-20秒内感知到并触发 Disconnect 事件
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] { 10000, 10000 })
                .setTaskScheduler(heartBeatScheduler());

        // 为特定用户发送消息的目标前缀
        registry.setUserDestinationPrefix("/user");
    }

    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("wss-heartbeat-thread-");
        scheduler.initialize();
        return scheduler;
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
                // Always wrap to ensure we have a fresh accessor that modifies the message
                // headers
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    log.info("【WS调试】检测到 CONNECT 连接请求！准备提取 Token...");

                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    // Try lowercase header if uppercase not found
                    if (authHeader == null) {
                        authHeader = accessor.getFirstNativeHeader("authorization");
                    }

                    log.info("【WS调试】提取到的 Authorization 头: {}", authHeader);

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        boolean isValid = jwtTokenProvider.validateToken(token)
                                && jwtTokenProvider.isAccessToken(token)
                                && !tokenRevocationService.isRevoked(token);
                        log.info("【WS调试】Token 校验结果: {}", isValid);

                        if (isValid) {
                            String username = jwtTokenProvider.getUsernameFromToken(token);
                            log.info("【WS调试】解析出的用户名: {}", username);

                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            if (!userDetails.isEnabled()) {
                                log.warn("【WS调试】⚠️ 用户已被禁用，拒绝建立 WS 连接: {}", username);
                                return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
                            }
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                            accessor.setUser(authentication);

                            // 关键修改：同时将用户名和用户ID存入 Session Attributes，作为双重保险
                            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                            if (sessionAttributes != null) {
                                sessionAttributes.put("username", username);
                                if (userDetails instanceof UserPrincipal) {
                                    sessionAttributes.put("userId", ((UserPrincipal) userDetails).getId());
                                }
                            }

                            log.info("【WS调试】✅ 成功设置用户认证信息: {}", username);
                        } else {
                            log.error("【WS调试】❌ Token 无效！");
                        }
                    } else {
                        log.warn("【WS调试】⚠️ Authorization 头为空或格式不对 (必须以 'Bearer ' 开头)");
                    }
                }
                // Ensure we return a message with the modified accessor headers
                return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
            }
        });
    }

    @Component
    @RequiredArgsConstructor
    public static class WebSocketEventListener {
        private final SimpMessagingTemplate messagingTemplate;
        private final Map<String, String> sessionIdToUserMap = new ConcurrentHashMap<>();
        private final Map<String, Long> sessionIdToUserIdMap = new ConcurrentHashMap<>();

        @EventListener
        public void handleWebSocketConnectListener(SessionConnectedEvent event) {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();

            String username = null;
            Long userId = null;

            // 1. 尝试从 Principal 获取
            if (event.getUser() instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) event.getUser();
                if (auth.getPrincipal() instanceof UserPrincipal) {
                    UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
                    username = principal.getUsername();
                    userId = principal.getId();
                } else {
                    username = event.getUser().getName();
                }
            } else if (event.getUser() != null) {
                username = event.getUser().getName();
            }

            // 2. 尝试从 Session Attributes 获取 (如果 Principal 为空或没拿到 ID)
            if (username == null || userId == null) {
                Map<String, Object> sessionAttributes = getSessionAttributes(headerAccessor);
                if (sessionAttributes != null) {
                    if (username == null && sessionAttributes.containsKey("username")) {
                        username = (String) sessionAttributes.get("username");
                    }
                    if (userId == null && sessionAttributes.containsKey("userId")) {
                        userId = (Long) sessionAttributes.get("userId");
                    }
                }
            }

            if (username != null && userId != null) {
                log.info("【WS事件】用户已连接: {} (ID: {}, Session: {})", username, userId, sessionId);
                sessionIdToUserMap.put(sessionId, username);
                sessionIdToUserIdMap.put(sessionId, userId);

                // 广播上线消息 (注意：前端监听的是 /topic/presence，且需要 userId)
                messagingTemplate.convertAndSend("/topic/presence",
                        Map.of("userId", userId, "status", "ONLINE"));
            } else {
                log.warn("【WS事件】连接事件中无法获取完整用户信息. Username: {}, UserId: {}, Session: {}", username, userId, sessionId);
            }
        }

        @EventListener
        public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
            String sessionId = event.getSessionId();
            String username = null;
            Long userId = null;

            // 1. 尝试从 Principal 获取
            if (event.getUser() instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) event.getUser();
                if (auth.getPrincipal() instanceof UserPrincipal) {
                    UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
                    username = principal.getUsername();
                    userId = principal.getId();
                }
            }
            if (username == null && event.getUser() != null) {
                username = event.getUser().getName();
            }

            // 2. 尝试从 Map 获取
            if (username == null)
                username = sessionIdToUserMap.get(sessionId);
            if (userId == null)
                userId = sessionIdToUserIdMap.get(sessionId);

            // 3. 尝试从 Session Attributes 获取
            if (username == null || userId == null) {
                StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
                Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
                if (sessionAttributes != null) {
                    if (username == null && sessionAttributes.containsKey("username")) {
                        username = (String) sessionAttributes.get("username");
                    }
                    if (userId == null && sessionAttributes.containsKey("userId")) {
                        userId = (Long) sessionAttributes.get("userId");
                    }
                }
            }

            if (userId != null) {
                log.info("【WS事件】用户已断开: {} (ID: {}, Session: {})", username, userId, sessionId);
                // 广播下线消息
                messagingTemplate.convertAndSend("/topic/presence",
                        Map.of("userId", userId, "status", "OFFLINE"));

                sessionIdToUserMap.remove(sessionId);
                sessionIdToUserIdMap.remove(sessionId);
            } else {
                log.warn("【WS事件】未知会话断开: {}", sessionId);
            }
        }

        private Map<String, Object> getSessionAttributes(StompHeaderAccessor accessor) {
            Map<String, Object> attrs = accessor.getSessionAttributes();
            if (attrs != null)
                return attrs;

            Message<?> connectMessage = (Message<?>) accessor.getHeader("simpConnectMessage");
            if (connectMessage != null) {
                StompHeaderAccessor connectAccessor = StompHeaderAccessor.wrap(connectMessage);
                return connectAccessor.getSessionAttributes();
            }
            return null;
        }
    }
}
