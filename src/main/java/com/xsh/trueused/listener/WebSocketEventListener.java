package com.xsh.trueused.listener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.xsh.trueused.security.user.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    // In-memory storage for online users (userId -> status)
    // Using a static map so it can be accessed by controllers if needed,
    // though dependency injection of a Service wrapping this would be cleaner.
    // For now, let's keep it simple.
    public static final Map<Long, Boolean> ONLINE_USERS = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        // The user is set in the ChannelInterceptor in WebSocketConfig
        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken user = (UsernamePasswordAuthenticationToken) headerAccessor.getUser();
            if (user.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) user.getPrincipal();
                Long userId = userPrincipal.getId();
                ONLINE_USERS.put(userId, true);
                log.info("User connected: {}", userId);

                // Broadcast user online event
                messagingTemplate.convertAndSend("/topic/presence", Map.of("userId", userId, "status", "ONLINE"));
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken user = (UsernamePasswordAuthenticationToken) headerAccessor.getUser();
            if (user.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) user.getPrincipal();
                Long userId = userPrincipal.getId();
                ONLINE_USERS.remove(userId);
                log.info("User disconnected: {}", userId);

                // Broadcast user offline event
                messagingTemplate.convertAndSend("/topic/presence", Map.of("userId", userId, "status", "OFFLINE"));
            }
        }
    }
}
