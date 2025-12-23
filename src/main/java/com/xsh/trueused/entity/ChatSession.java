package com.xsh.trueused.entity;

import com.xsh.trueused.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chat_session", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id_a", "user_id_b"})
})
public class ChatSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_a", nullable = false)
    private User userA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_b", nullable = false)
    private User userB;

    @Column(name = "last_message_content", length = 1000)
    private String lastMessageContent;

    @Column(name = "last_message_time")
    private Instant lastMessageTime;

    @Column(name = "unread_count_a", nullable = false)
    private int unreadCountA = 0;

    @Column(name = "unread_count_b", nullable = false)
    private int unreadCountB = 0;
}
