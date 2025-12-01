package com.xsh.trueused.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.dto.ConversationDTO;
import com.xsh.trueused.entity.Conversation;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.repository.ConversationRepository;
import com.xsh.trueused.repository.MessageRepository;
import com.xsh.trueused.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository; // For unread count if needed, though maybe complex

    @Transactional(readOnly = true)
    public List<ConversationDTO> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId);
        List<ConversationDTO> dtos = new ArrayList<>();

        for (Conversation c : conversations) {
            ConversationDTO dto = new ConversationDTO();
            dto.setId(c.getId());

            User otherUser = c.getParticipant1().getId().equals(userId) ? c.getParticipant2() : c.getParticipant1();
            dto.setOtherUserId(otherUser.getId());
            dto.setOtherUserName(otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getUsername());
            dto.setOtherUserAvatar(otherUser.getAvatarUrl());

            if (c.getLastMessage() != null) {
                dto.setLastMessage(c.getLastMessage().getContent());
                dto.setLastMessageTime(c.getLastMessage().getCreatedAt());
            }

            long unread = messageRepository.countByConversationIdAndReceiverIdAndIsReadFalse(c.getId(), userId);
            dto.setUnreadCount((int) unread);

            dtos.add(dto);
        }

        // Sort by last message time desc
        dtos.sort((a, b) -> {
            if (a.getLastMessageTime() == null)
                return 1;
            if (b.getLastMessageTime() == null)
                return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        return dtos;
    }

    @Transactional
    public Conversation getOrCreateConversation(Long user1Id, Long user2Id) {
        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("Cannot start conversation with yourself");
        }

        Optional<Conversation> existing = conversationRepository.findByParticipants(user1Id, user2Id);
        if (existing.isPresent()) {
            return existing.get();
        }

        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found: " + user1Id));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found: " + user2Id));

        Conversation conversation = new Conversation();
        conversation.setParticipant1(user1);
        conversation.setParticipant2(user2);

        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public Conversation getConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Conversation not found"));
    }
}
