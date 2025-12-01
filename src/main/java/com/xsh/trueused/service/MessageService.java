package com.xsh.trueused.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.dto.ChatMessageDTO;
import com.xsh.trueused.entity.Conversation;
import com.xsh.trueused.entity.Message;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.repository.ConversationRepository;
import com.xsh.trueused.repository.MessageRepository;
import com.xsh.trueused.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ConversationService conversationService;

    @Transactional
    public ChatMessageDTO saveMessage(Long senderId, Long receiverId, String content) {
        log.info("Saving message from {} to {}: {}", senderId, receiverId, content);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Conversation conversation = conversationService.getOrCreateConversation(senderId, receiverId);
        log.info("Using conversation ID: {}", conversation.getId());

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setRead(false);

        Message savedMessage = messageRepository.save(message);
        log.info("Message saved to repository with ID: {}", savedMessage.getId());

        // Update conversation last message
        conversation.setLastMessage(savedMessage);
        conversationRepository.save(conversation);
        log.info("Conversation updated with last message ID: {}", savedMessage.getId());

        return convertToDTO(savedMessage, senderId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getMessages(Long conversationId, Long currentUserId, Pageable pageable) {
        Conversation conversation = conversationService.getConversation(conversationId);

        // Security check
        if (!conversation.getParticipant1().getId().equals(currentUserId) &&
                !conversation.getParticipant2().getId().equals(currentUserId)) {
            throw new RuntimeException("Access denied");
        }

        Page<Message> messagePage = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId,
                pageable);

        // We want to return them in chronological order for the chat view usually,
        // but the query was Desc for pagination (getting latest first).
        // So we might want to reverse them or just let the frontend handle it.
        // Usually APIs return latest page.

        return messagePage.getContent().stream()
                .map(msg -> convertToDTO(msg, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long conversationId, Long userId) {
        messageRepository.markMessagesAsRead(conversationId, userId);
    }

    private ChatMessageDTO convertToDTO(Message message, Long currentUserId) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getCreatedAt());
        dto.setRead(message.isRead());
        dto.setSelf(message.getSender().getId().equals(currentUserId));
        return dto;
    }
}
