package com.xsh.trueused.chat.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class ConversationDTO {
    private Long id;
    private Long otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private String lastMessage;
    private Instant lastMessageTime;
    private int unreadCount;
}
