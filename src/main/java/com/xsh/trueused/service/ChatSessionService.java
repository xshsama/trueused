package com.xsh.trueused.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.xsh.trueused.dto.ConversationDTO;
import com.xsh.trueused.entity.ChatSession;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.repository.ChatSessionRepository;
import com.xsh.trueused.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ConversationDTO> getUserConversations(Long userId) {
        List<ChatSession> sessions = chatSessionRepository.findByUserId(userId);
        List<ConversationDTO> dtos = new ArrayList<>();

        for (ChatSession s : sessions) {
            ConversationDTO dto = new ConversationDTO();
            dto.setId(s.getId());

            User otherUser = s.getUserA().getId().equals(userId) ? s.getUserB() : s.getUserA();
            dto.setOtherUserId(otherUser.getId());
            dto.setOtherUserName(otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getUsername());
            dto.setOtherUserAvatar(otherUser.getAvatarUrl());

            dto.setLastMessage(s.getLastMessageContent());
            dto.setLastMessageTime(s.getLastMessageTime());

            // Get unread count from the denormalized column
            if (s.getUserA().getId().equals(userId)) {
                dto.setUnreadCount(s.getUnreadCountA());
            } else {
                dto.setUnreadCount(s.getUnreadCountB());
            }

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
    public ChatSession getOrCreateChatSession(Long user1Id, Long user2Id) {
        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("Cannot start conversation with yourself");
        }

        Optional<ChatSession> existing = chatSessionRepository.findByParticipants(user1Id, user2Id);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Enforce ID ordering for new sessions (userA < userB)
        Long idA = Math.min(user1Id, user2Id);
        Long idB = Math.max(user1Id, user2Id);

        User userA = userRepository.findById(idA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + idA));
        User userB = userRepository.findById(idB)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + idB));

        ChatSession session = new ChatSession();
        session.setUserA(userA);
        session.setUserB(userB);

        return chatSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public ChatSession getChatSession(Long sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat session not found"));
    }
}
