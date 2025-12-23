package com.xsh.trueused.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.ChatSession;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    @Query("SELECT c FROM ChatSession c WHERE (c.userA.id = :userId OR c.userB.id = :userId)")
    List<ChatSession> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM ChatSession c WHERE (c.userA.id = :user1Id AND c.userB.id = :user2Id) OR (c.userA.id = :user2Id AND c.userB.id = :user1Id)")
    Optional<ChatSession> findByParticipants(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
