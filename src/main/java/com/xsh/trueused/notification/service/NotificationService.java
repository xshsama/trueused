package com.xsh.trueused.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.entity.Notification;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.notification.repository.NotificationRepository;
import com.xsh.trueused.user.repository.UserRepository;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void createNotification(Long userId, String title, String content, String type, Long relatedId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRelatedId(relatedId);

        notificationRepository.save(notification);

        // Send WebSocket notification
        // Client subscribes to /user/queue/notifications
        try {
            // Send a plain payload to avoid serializing JPA lazy proxies (e.g. notification.user).
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("id", notification.getId());
            payload.put("userId", user.getId());
            payload.put("title", notification.getTitle());
            payload.put("content", notification.getContent());
            payload.put("type", notification.getType());
            payload.put("relatedId", notification.getRelatedId());
            payload.put("isRead", notification.isRead());
            payload.put("createdAt", notification.getCreatedAt());

            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications",
                    payload);
        } catch (MessagingException | IllegalArgumentException ex) {
            // Notification push failure must not break main transaction (e.g. order creation).
            log.warn("Failed to push websocket notification, userId={}, notificationId={}",
                    user.getId(), notification.getId(), ex);
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        var notifications = notificationRepository.findByUser_IdAndIsReadFalse(userId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }
}
