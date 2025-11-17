package com.xsh.trueused.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import com.xsh.trueused.security.user.UserPrincipal;

@Controller
public class MessageController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public MessageController(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public static record ChatMessagePayload(Long receiverId, String content) {
    }

    @MessageMapping("/chat")
    public void sendChatMessage(@Payload ChatMessagePayload payload, @AuthenticationPrincipal UserPrincipal sender) {
        // 在实际应用中，这里应该包含以下逻辑：
        // 1. 验证 sender 和 payload.receiverId() 是否有效。
        // 2. 查找或创建一个 Conversation。
        // 3. 创建一个新的 Message 实体并保存到数据库。
        // 4. 更新 Conversation 的 last_message_id。

        // 为了演示，我们暂时只实现消息的实时转发
        // 将消息发送到指定接收者的私有队列
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(payload.receiverId()),
                "/queue/messages",
                payload.content());
    }
}