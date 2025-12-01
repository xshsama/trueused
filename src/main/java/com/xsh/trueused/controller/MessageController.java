package com.xsh.trueused.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import com.xsh.trueused.dto.ChatMessageDTO;
import com.xsh.trueused.dto.SendMessageRequest;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.repository.UserRepository;
import com.xsh.trueused.security.user.UserPrincipal;
import com.xsh.trueused.service.MessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MessageController {

        private final SimpMessagingTemplate simpMessagingTemplate;
        private final MessageService messageService;
        private final UserRepository userRepository;

        @MessageMapping("/chat")
        public void sendChatMessage(@Payload SendMessageRequest request,
                        @AuthenticationPrincipal UserPrincipal sender) {
                if (sender == null) {
                        log.error("Sender is null. Authentication might have failed.");
                        throw new RuntimeException("Unauthorized");
                }
                log.info("Received chat message from user: {}", sender.getUsername());
                log.info("Receiver ID: {}, Content: {}", request.getReceiverId(), request.getContent());

                // Save message to DB
                ChatMessageDTO savedMessage = messageService.saveMessage(sender.getId(), request.getReceiverId(),
                                request.getContent());

                log.info("Message saved with ID: {}", savedMessage.getId());

                // Find receiver's username to send via WebSocket
                // Note: This assumes the receiver is connected with this username as Principal
                User receiver = userRepository.findById(request.getReceiverId())
                                .orElseThrow(() -> new RuntimeException("Receiver not found"));

                log.info("Sending to receiver: {} (ID: {})", receiver.getUsername(), receiver.getId());
                // Send to receiver via topic (more reliable than user queue)
                simpMessagingTemplate.convertAndSend(
                                "/topic/user/" + receiver.getId(),
                                savedMessage);

                log.info("Sending echo to sender: {} (ID: {})", sender.getUsername(), sender.getId());
                // Send echo to sender via topic
                simpMessagingTemplate.convertAndSend(
                                "/topic/user/" + sender.getId(),
                                savedMessage);
        }
}