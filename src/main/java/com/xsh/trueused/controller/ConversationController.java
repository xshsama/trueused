package com.xsh.trueused.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.ChatMessageDTO;
import com.xsh.trueused.dto.ConversationDTO;
import com.xsh.trueused.entity.ChatSession;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.ChatSessionService;
import com.xsh.trueused.service.ChatMessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    @GetMapping
    public ResponseEntity<List<ConversationDTO>> getConversations(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<ConversationDTO> conversations = chatSessionService.getUserConversations(currentUser.getId());
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        List<ChatMessageDTO> messages = chatMessageService.getMessages(sessionId, currentUser.getId(), pageable);

        // Mark as read when fetching
        chatMessageService.markAsRead(sessionId, currentUser.getId());

        return ResponseEntity.ok(messages);
    }

    @PostMapping("/start")
    public ResponseEntity<ConversationDTO> startConversation(
            @RequestParam Long userId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ChatSession session = chatSessionService.getOrCreateChatSession(currentUser.getId(), userId);

        // Convert to DTO
        ConversationDTO dto = new ConversationDTO();
        dto.setId(session.getId());

        User otherUser = session.getUserA().getId().equals(currentUser.getId())
                ? session.getUserB()
                : session.getUserA();

        dto.setOtherUserId(otherUser.getId());
        dto.setOtherUserName(otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getUsername());
        dto.setOtherUserAvatar(otherUser.getAvatarUrl());

        dto.setLastMessage(session.getLastMessageContent());
        dto.setLastMessageTime(session.getLastMessageTime());
        
        // Unread count (probably 0 for new session or whatever is in DB)
        if (session.getUserA().getId().equals(currentUser.getId())) {
            dto.setUnreadCount(session.getUnreadCountA());
        } else {
            dto.setUnreadCount(session.getUnreadCountB());
        }

        return ResponseEntity.ok(dto);
    }
}
