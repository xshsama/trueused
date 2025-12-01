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
import com.xsh.trueused.entity.Conversation;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.ConversationService;
import com.xsh.trueused.service.MessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<ConversationDTO>> getConversations(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<ConversationDTO> conversations = conversationService.getUserConversations(currentUser.getId());
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        List<ChatMessageDTO> messages = messageService.getMessages(conversationId, currentUser.getId(), pageable);

        // Mark as read when fetching
        messageService.markAsRead(conversationId, currentUser.getId());

        return ResponseEntity.ok(messages);
    }

    @PostMapping("/start")
    public ResponseEntity<ConversationDTO> startConversation(
            @RequestParam Long userId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Conversation conversation = conversationService.getOrCreateConversation(currentUser.getId(), userId);

        // Convert to DTO
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());

        User otherUser = conversation.getParticipant1().getId().equals(currentUser.getId())
                ? conversation.getParticipant2()
                : conversation.getParticipant1();

        dto.setOtherUserId(otherUser.getId());
        dto.setOtherUserName(otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getUsername());
        dto.setOtherUserAvatar(otherUser.getAvatarUrl());

        if (conversation.getLastMessage() != null) {
            dto.setLastMessage(conversation.getLastMessage().getContent());
            dto.setLastMessageTime(conversation.getLastMessage().getCreatedAt());
        }

        return ResponseEntity.ok(dto);
    }
}