package com.xsh.trueused.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.ChatMessageDTO;
import com.xsh.trueused.dto.SendMessageRequest;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.repository.UserRepository;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.MessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

        private final SimpMessagingTemplate simpMessagingTemplate;
        private final MessageService messageService;
        private final UserRepository userRepository;

        @PostMapping
        public ResponseEntity<ChatMessageDTO> sendMessage(@RequestBody SendMessageRequest request,
                        @AuthenticationPrincipal UserPrincipal sender) {
                if (sender == null) {
                        throw new RuntimeException("Unauthorized");
                }

                // Save message to DB
                ChatMessageDTO savedMessage = messageService.saveMessage(sender.getId(), request.getReceiverId(),
                                request.getContent());

                // Find receiver's username to send via WebSocket
                User receiver = userRepository.findById(request.getReceiverId())
                                .orElseThrow(() -> new RuntimeException("Receiver not found"));

                // Send to receiver via topic
                simpMessagingTemplate.convertAndSend(
                                "/topic/user/" + receiver.getId(),
                                savedMessage);

                return ResponseEntity.ok(savedMessage);
        }
}
