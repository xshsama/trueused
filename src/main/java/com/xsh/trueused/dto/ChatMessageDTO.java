package com.xsh.trueused.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class ChatMessageDTO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Instant timestamp;
    private boolean isRead;
    private boolean isSelf; // Helper for frontend
}
