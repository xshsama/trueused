package com.xsh.trueused.chat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.chat.dto.ChatMessageDTO;
import com.xsh.trueused.entity.ChatMessage;
import com.xsh.trueused.entity.ChatSession;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.chat.repository.ChatMessageRepository;
import com.xsh.trueused.chat.repository.ChatSessionRepository;
import com.xsh.trueused.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final ChatSessionService chatSessionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    @Transactional
    public ChatMessageDTO saveMessage(Long senderId, Long receiverId, String content) {
        log.info("Saving message from {} to {}: {}", senderId, receiverId, content);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        ChatSession session = chatSessionService.getOrCreateChatSession(senderId, receiverId);
        log.info("Using chat session ID: {}", session.getId());

        ChatMessage message = new ChatMessage();
        message.setChatSession(session);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setRead(false);

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("Message saved to repository with ID: {}", savedMessage.getId());

        // Update session snapshot
        session.setLastMessageContent(content);
        session.setLastMessageTime(savedMessage.getCreatedAt());

        // Check if receiver is online
        boolean isReceiverOnline = userRegistry.getUser(receiver.getUsername()) != null;

        // Debug log for online users
        log.info("Checking online status for: {}. Is Online: {}", receiver.getUsername(), isReceiverOnline);
        log.info("Current online users: {}", userRegistry.getUsers().stream()
                .map(u -> u.getName())
                .collect(Collectors.toList()));

        // Only increment unread count if receiver is OFFLINE
        if (!isReceiverOnline) {
            if (session.getUserA().getId().equals(senderId)) {
                session.setUnreadCountB(session.getUnreadCountB() + 1);
            } else {
                session.setUnreadCountA(session.getUnreadCountA() + 1);
            }
        }

        chatSessionRepository.save(session);
        log.info("ChatSession updated. Receiver online: {}", isReceiverOnline);

        ChatMessageDTO messageDTO = convertToDTO(savedMessage, senderId);

        if (isReceiverOnline) {
            // Real-time push to receiver
            messagingTemplate.convertAndSendToUser(
                    receiver.getUsername(),
                    "/queue/messages",
                    messageDTO);
            log.info("Message pushed to online user: {}", receiver.getUsername());
        } else {
            log.info("User {} is offline, message saved but not pushed", receiver.getUsername());
        }

        return messageDTO;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getMessages(Long sessionId, Long currentUserId, Pageable pageable) {
        ChatSession session = chatSessionService.getChatSession(sessionId);

        // Security check
        if (!session.getUserA().getId().equals(currentUserId) &&
                !session.getUserB().getId().equals(currentUserId)) {
            throw new RuntimeException("Access denied");
        }

        Page<ChatMessage> messagePage = chatMessageRepository.findByChatSessionIdOrderByCreatedAtDesc(sessionId,
                pageable);

        return messagePage.getContent().stream()
                .map(msg -> convertToDTO(msg, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long sessionId, Long userId) {
        // Mark messages as read in message table
        chatMessageRepository.markMessagesAsRead(sessionId, userId);

        // Reset unread count in session table
        ChatSession session = chatSessionService.getChatSession(sessionId);
        if (session.getUserA().getId().equals(userId)) {
            session.setUnreadCountA(0);
        } else if (session.getUserB().getId().equals(userId)) {
            session.setUnreadCountB(0);
        }
        chatSessionRepository.save(session);
    }

    private ChatMessageDTO convertToDTO(ChatMessage message, Long currentUserId) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getChatSession().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getCreatedAt());
        dto.setRead(message.isRead());
        dto.setSelf(message.getSender().getId().equals(currentUserId));
        return dto;
    }
}
