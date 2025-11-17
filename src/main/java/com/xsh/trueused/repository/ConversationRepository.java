package com.xsh.trueused.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}