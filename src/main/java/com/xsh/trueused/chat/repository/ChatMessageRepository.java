package com.xsh.trueused.chat.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatSessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long sessionId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.chatSession.id = :sessionId AND m.receiver.id = :userId AND m.isRead = false")
    void markMessagesAsRead(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    long countByChatSessionIdAndReceiverIdAndIsReadFalse(Long sessionId, Long receiverId);
}
