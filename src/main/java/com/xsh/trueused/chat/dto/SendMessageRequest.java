package com.xsh.trueused.chat.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private Long receiverId;
    private String content;
}
