package com.xsh.trueused.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.entity.Conversation;
import com.xsh.trueused.entity.Message;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    // 在实际应用中，这里应该注入 ConversationService 和 MessageService

    @GetMapping
    public ResponseEntity<List<Conversation>> getConversations() {
        // TODO: 实现获取当前用户会话列表的逻辑
        // 1. 从 SecurityContext 获取当前用户ID。
        // 2. 查询数据库中 participant1_id 或 participant2_id 为当前用户ID的所有会话。
        // 3. 按 lastMessage 的时间戳降序排序。
        // 4. 返回会话列表的 DTO。
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<Message>> getMessages(@PathVariable Long conversationId) {
        // TODO: 实现获取会话历史消息的逻辑
        // 1. 验证当前用户是否是该会话的参与者之一。
        // 2. 根据 conversationId 查询消息，并按时间升序排序。
        // 3. 实现分页逻辑。
        // 4. 返回消息列表的 DTO。
        return ResponseEntity.ok(Collections.emptyList());
    }
}