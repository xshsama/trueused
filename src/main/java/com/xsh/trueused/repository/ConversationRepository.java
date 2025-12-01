package com.xsh.trueused.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE (c.participant1.id = :userId OR c.participant2.id = :userId)")
    List<Conversation> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE (c.participant1.id = :user1Id AND c.participant2.id = :user2Id) OR (c.participant1.id = :user2Id AND c.participant2.id = :user1Id)")
    Optional<Conversation> findByParticipants(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}